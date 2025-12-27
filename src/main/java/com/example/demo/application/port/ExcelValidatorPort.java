package com.example.demo.application.port;

import org.springframework.web.multipart.MultipartFile;

public interface ExcelValidatorPort {

	/**
	 * 驗證 Excel 資料
	 * 
	 * @param templateName 範本名稱
	 * @param file         MultiPart 檔案資料
	 */
	void validateExcelData(String templateName, MultipartFile file);
}
