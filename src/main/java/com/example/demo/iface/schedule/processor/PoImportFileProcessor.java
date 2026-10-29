package com.example.demo.iface.schedule.processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.stereotype.Component;

import com.example.demo.application.annotation.CustomValidate;
import com.example.demo.application.service.UploadCommandService;
import com.example.demo.application.shared.command.UploadTemplateCommand;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 採購訂單匯入批次檔案處理器 (PO Import File Processor - Inbound Adapter)
 * <p>
 * 作為排程/批次系統之輸入適配器處理單元，實作 {@link BatchFileProcessor} 介面，負責處理採購訂單 (Purchase Order) Excel 檔案。<br>
 * 支援以下兩種輸入管道：
 * <ul>
 *   <li>本機/SFTP 檔案：{@link #processFile(File)}</li>
 *   <li>MinIO / AWS S3 等物件儲存系統二進位串流：{@link #processStream(InputStream)}</li>
 * </ul>
 * 透過 {@link CustomValidate} 宣告式觸發核心 AOP 切面檢核，
 * 確保任何採購單檔案在進入應用層 {@link UploadCommandService} 之前，
 * 已經完成完整的雙層防腐正規化 (工作表與欄位標題代碼對映) 與 SpEL 商業規則校驗。
 * </p>
 */
@Slf4j
@Component("poImportFileProcessor")
@RequiredArgsConstructor
public class PoImportFileProcessor implements BatchFileProcessor {

	private static final String TEMPLATE_NAME = "PO_IMPORT";

	private final UploadCommandService uploadCommandService;

	@Override
	public String getTemplateName() {
		return TEMPLATE_NAME;
	}

	/**
	 * 處理單一採購訂單 Excel 實體檔案
	 * <p>
	 * 標註 {@code @CustomValidate(templateName = "PO_IMPORT")}：<br>
	 * 執行前由 {@link com.example.demo.iface.aspect.CustomValidationAspect} 動態提取傳入之 {@link File}，
	 * 針對 {@code PO_IMPORT} 範本規則進行資料校驗。<br>
	 * 若檢核失敗，AOP 切面將拋出 {@link com.example.demo.application.exception.ExcelValidationException}，
	 * 阻擋後續應用層入庫執行。
	 * </p>
	 *
	 * @param excelFile 待處理之 Excel 檔案
	 * @throws IOException 當檔案讀取發生 I/O 異常時拋出
	 */
	@Override
	@CustomValidate(templateName = TEMPLATE_NAME)
	public void processFile(File excelFile) throws IOException {
		log.info("採購訂單檔案 [{}] 通過 @CustomValidate 檢核，開始發送命令至應用層進行入庫處理...", excelFile.getName());

		UploadTemplateCommand command = new UploadTemplateCommand(
				getTemplateName(),
				"PO",
				"xlsx",
				excelFile.getParent(),
				excelFile.getName()
		);

		try (FileInputStream inputStream = new FileInputStream(excelFile)) {
			uploadCommandService.upload(command, inputStream);
		}

		log.info("採購訂單檔案 [{}] 應用層匯入處理完成", excelFile.getName());
	}

	/**
	 * 處理採購訂單 Excel 輸入串流 (適用於 MinIO、AWS S3 物件儲存或網路串流)
	 * <p>
	 * 標註 {@code @CustomValidate(templateName = "PO_IMPORT")}：<br>
	 * AOP 切面會自動將 {@link InputStream} 包裝為支援重設之緩衝串流，執行驗證後保持串流可讀狀態傳遞至應用層。
	 * </p>
	 *
	 * @param inputStream Excel 二進位串流
	 * @throws IOException 當串流讀取發生異常時拋出
	 */
	@Override
	@CustomValidate(templateName = TEMPLATE_NAME)
	public void processStream(InputStream inputStream) throws IOException {
		log.info("採購訂單 Excel 串流通過 @CustomValidate 檢核，開始發送命令至應用層進行入庫處理...");

		UploadTemplateCommand command = new UploadTemplateCommand(
				getTemplateName(),
				"PO",
				"xlsx",
				null,
				"STREAM_PO_IMPORT.xlsx"
		);

		uploadCommandService.upload(command, inputStream);
		log.info("採購訂單 Excel 串流應用層匯入處理完成");
	}

	/**
	 * 判斷該處理器是否支援處理傳入之檔案
	 * <p>
	 * 預設匹配檔名以 "PO" 開頭（不分大小寫）之 Excel 試算表。
	 * </p>
	 *
	 * @param file 待檢核檔案
	 * @return true 代表該檔案屬於此處理器職責範疇
	 */
	@Override
	public boolean supports(File file) {
		if (file == null || !file.exists()) {
			return false;
		}
		String name = file.getName().toUpperCase();
		return name.startsWith("PO") && (name.endsWith(".XLSX") || name.endsWith(".XLS"));
	}
}
