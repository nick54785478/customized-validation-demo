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
 * 請購匯入批次檔案處理器 (PR Import File Processor - Inbound Adapter)
 * <p>
 * 作為排程系統之輸入適配器處理單元，實作 {@link BatchFileProcessor} 介面，負責處理單一請購 Excel 檔案。
 * 透過繼承之 {@link CustomValidate} 宣告式觸發核心 AOP 切面檢核，
 * 確保任何排程檔案在進入核心應用層 {@link UploadCommandService} 之前，
 * 已經完成完整的欄位映射與業務防腐檢核。
 * </p>
 */
@Slf4j
@Component("prImportFileProcessor")
@RequiredArgsConstructor
public class PrImportFileProcessor implements BatchFileProcessor {

	private static final String TEMPLATE_NAME = "PR_IMPORT";

	private final UploadCommandService uploadCommandService;

	@Override
	public String getTemplateName() {
		return TEMPLATE_NAME;
	}

	/**
	 * 處理單一請購 Excel 檔案
	 * <p>
	 * 標註 {@code @CustomValidate(templateName = "PR_IMPORT")}：
	 * 執行前由 {@link com.example.demo.iface.aspect.CustomValidationAspect} 動態提取傳入之 {@link File}，
	 * 針對 {@code PR_IMPORT} 範本規則進行資料校驗。
	 * 若檢核失敗，AOP 切面將拋出 {@link com.example.demo.application.exception.ExcelValidationException}，
	 * 阻擋後續邏輯執行。
	 * </p>
	 *
	 * @param excelFile 待處理之 Excel 檔案
	 * @throws IOException 當檔案讀取發生 I/O 異常時拋出
	 */
	@Override
	@CustomValidate(templateName = TEMPLATE_NAME)
	public void processFile(File excelFile) throws IOException {
		log.info("檔案 [{}] 通過 @CustomValidate 檢核，開始發送命令至應用層進行入庫處理...", excelFile.getName());

		UploadTemplateCommand command = new UploadTemplateCommand(
				getTemplateName(),
				"PR",
				"xlsx",
				excelFile.getParent(),
				excelFile.getName()
		);

		try (FileInputStream inputStream = new FileInputStream(excelFile)) {
			uploadCommandService.upload(command, inputStream);
		}

		log.info("檔案 [{}] 應用層匯入處理完成", excelFile.getName());
	}

	/**
	 * 處理請購 Excel 輸入串流 (適用於 MinIO、S3 物件儲存或網路串流)
	 *
	 * @param inputStream Excel 二進位串流
	 * @throws IOException 當串流讀取發生異常時拋出
	 */
	@Override
	@CustomValidate(templateName = TEMPLATE_NAME)
	public void processStream(InputStream inputStream) throws IOException {
		log.info("請購 Excel 串流通過 @CustomValidate 檢核，開始發送命令至應用層進行入庫處理...");

		UploadTemplateCommand command = new UploadTemplateCommand(
				getTemplateName(),
				"PR",
				"xlsx",
				null,
				"STREAM_IMPORT.xlsx"
		);

		uploadCommandService.upload(command, inputStream);
		log.info("請購 Excel 串流應用層匯入處理完成");
	}

	@Override
	public boolean supports(File file) {
		if (file == null || !file.exists()) {
			return false;
		}
		String name = file.getName().toUpperCase();
		return name.startsWith("PR") || name.contains(getTemplateName().toUpperCase());
	}
}
