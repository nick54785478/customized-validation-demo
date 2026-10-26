package com.example.demo.application.port.out;

import java.io.InputStream;

/**
 * Excel 驗證器輸出連接埠介面 (Excel Validator Port - Outbound Port)
 * <p>
 * 遵循六角形架構規範，本介面作為應用層要求基礎設施層提供驗證實作的輸出連接埠 (Outbound Port)。
 * 參數傳入標準 Java {@link InputStream}，隔絕任何外部技術與傳輸協定依賴。
 * </p>
 */
public interface ExcelValidatorPort {

	/**
	 * 驗證 Excel 資料內容
	 *
	 * @param templateName 範本名稱
	 * @param inputStream  Excel 檔案輸入串流
	 */
	void validateExcelData(String templateName, InputStream inputStream);
}
