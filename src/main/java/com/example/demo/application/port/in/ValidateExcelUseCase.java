package com.example.demo.application.port.in;

import java.io.InputStream;

/**
 * Excel 範本驗證使用案例介面 (Validate Excel Use Case - Inbound Port)
 * <p>
 * 遵循六角形架構規範，本介面作為應用層對外公開的輸入連接埠 (Inbound Port / Use Case)。
 * 接收標準 Java 輸入串流進行範本檢核，嚴禁依賴任何外部 Web 框架物件 (如 {@code MultipartFile})。
 * </p>
 */
public interface ValidateExcelUseCase {

	/**
	 * 驗證 Excel 範本資料
	 *
	 * @param templateName 範本名稱 (例如: USER_PROFILE, DEPT_PROFILE)
	 * @param inputStream  Excel 檔案內容串流 (由外層負責生命週期管理)
	 */
	void validateExcel(String templateName, InputStream inputStream);
}
