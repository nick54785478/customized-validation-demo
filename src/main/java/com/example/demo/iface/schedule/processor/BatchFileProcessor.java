package com.example.demo.iface.schedule.processor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.example.demo.application.annotation.CustomValidate;

/**
 * 批次檔案處理器介面 (Batch File Processor Interface - Inbound Adapter)
 * <p>
 * 定義定時排程或批次目錄掃描中，各業務範本檔案處理之標準適配合約。
 * 遵循依賴反轉原則 (DIP) 與開放封閉原則 (OCP)，任何業務範本（如請購匯入 PR_IMPORT、使用者資料 USER_PROFILE 等）
 * 皆可透過實作此介面完成擴充。
 * 介面方法預設宣告 {@code @CustomValidate(templateName = "#this.templateName")}，
 * 使得任何實作類別皆自動繼承 AOP 前置資料防腐檢核機制。
 * </p>
 */
public interface BatchFileProcessor {

	/**
	 * 取得該處理器所屬之範本識別名稱 (例如: "PR_IMPORT")
	 *
	 * @return 範本名稱
	 */
	String getTemplateName();

	/**
	 * 處理單一 Excel 批次檔案 (本機或暫存檔案)
	 * <p>
	 * 預設標註 {@link CustomValidate}，透過 SpEL 動態取得實作類別之 {@link #getTemplateName()}，
	 * 由 AOP 切面在執行業務邏輯前，自動完成格式防腐與業務規則檢核。
	 * </p>
	 *
	 * @param excelFile 待處理之 Excel 檔案
	 * @throws IOException 當檔案讀取或 I/O 發生異常時拋出
	 */
	@CustomValidate(templateName = "#this.templateName")
	void processFile(File excelFile) throws IOException;

	/**
	 * 處理 Excel 二進位輸入串流 (適用於 MinIO、S3、SFTP 記憶體串流或訊息佇列來源)
	 * <p>
	 * 預設標註 {@link CustomValidate}，透過 SpEL 動態取得實作類別之 {@link #getTemplateName()}，
	 * 由 AOP 切面在執行業務邏輯前，自動完成格式防腐與業務規則檢核。
	 * </p>
	 *
	 * @param inputStream Excel 二進位串流
	 * @throws IOException 當串流讀取發生異常時拋出
	 */
	@CustomValidate(templateName = "#this.templateName")
	void processStream(InputStream inputStream) throws IOException;

	/**
	 * 判斷當前處理器是否支援處理指定檔案
	 * <p>
	 * 預設支援指定之非空檔案，具體實作類別可依業務需求 (例如檔名前綴、副檔名) 覆寫自訂過濾。
	 * </p>
	 *
	 * @param file 待處理之檔案
	 * @return true 代表該處理器支援此檔案
	 */
	default boolean supports(File file) {
		return file != null && file.exists();
	}
}
