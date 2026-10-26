package com.example.demo.iface.resource.in;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 範本上傳請求資料載體 (Upload Template Request Resource)
 * <p>
 * 遵循命名規範以 {@code V + N + Resource} 命名。
 * 作為表現層輸入純資料載體 (Pure Data Carrier)，禁止包含業務與模型轉換邏輯。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadTemplateResource {

	/**
	 * 範本名稱 (例如: USER_PROFILE, DEPT_PROFILE)
	 */
	private String name;

	/**
	 * 範本種類代碼
	 */
	private String type;

	/**
	 * 檔案格式種類 (例如: XLSX, CSV)
	 */
	private String fileType;

	/**
	 * 檔案伺服器暫存路徑 (選填)
	 */
	private String filePath;

	/**
	 * 原始檔案名稱
	 */
	private String fileName;

}
