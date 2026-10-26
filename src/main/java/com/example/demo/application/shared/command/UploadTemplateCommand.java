package com.example.demo.application.shared.command;

import java.io.Serializable;

/**
 * 範本上傳業務指令 (Upload Template Command)
 * <p>
 * 遵循命名規範以 {@code V + N + Command} 命名，以 Java 17 {@code record} 實作不可變之業務命令載體。
 * 位於應用層共享命令套件 (Application Shared Command)，封裝執行範本上傳用例所必需之參數資訊。
 * </p>
 *
 * @param name     範本名稱 (例如: USER_PROFILE, DEPT_PROFILE)
 * @param type     範本種類
 * @param fileType 檔案種類 (例如: XLSX, CSV)
 * @param filePath 檔案儲存路徑
 * @param fileName 原始檔案名稱
 */
public record UploadTemplateCommand(
		String name,
		String type,
		String fileType,
		String filePath,
		String fileName) implements Serializable {

	/**
	 * 取得範本名稱（JavaBean 兼容方法）
	 *
	 * @return 範本名稱
	 */
	public String getName() {
		return name;
	}

	/**
	 * 取得範本種類（JavaBean 兼容方法）
	 *
	 * @return 範本種類
	 */
	public String getType() {
		return type;
	}

	/**
	 * 取得檔案種類（JavaBean 兼容方法）
	 *
	 * @return 檔案種類
	 */
	public String getFileType() {
		return fileType;
	}

	/**
	 * 取得檔案儲存路徑（JavaBean 兼容方法）
	 *
	 * @return 檔案儲存路徑
	 */
	public String getFilePath() {
		return filePath;
	}

	/**
	 * 取得原始檔案名稱（JavaBean 兼容方法）
	 *
	 * @return 原始檔案名稱
	 */
	public String getFileName() {
		return fileName;
	}
}
