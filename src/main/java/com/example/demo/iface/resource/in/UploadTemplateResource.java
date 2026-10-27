package com.example.demo.iface.resource.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 範本上傳請求資料載體 (Upload Template Request Resource)
 * <p>
 * 遵循命名規範以 {@code V + N + Resource} 命名。
 * 作為表現層輸入純資料載體 (Pure Data Carrier)，禁止包含業務與模型轉換邏輯。
 * </p>
 *
 * @param name     範本名稱 (例如: PR_IMPORT, USER_PROFILE)
 * @param type     範本種類代碼 (例如: PR, EXCEL)
 * @param fileType 檔案格式種類 (例如: xlsx, csv)
 * @param filePath 檔案伺服器暫存路徑 (選填)
 * @param fileName 原始檔案名稱
 */
@Schema(
		name = "UploadTemplateResource",
		description = "範本上傳請求中繼資訊資料載體",
		example = """
				{
				  "name": "PR_IMPORT",
				  "type": "PR",
				  "fileType": "xlsx",
				  "filePath": "/TEMPLATE/PR_IMPORT",
				  "fileName": "PR_IMPORT.xlsx"
				}
				"""
)
public record UploadTemplateResource(
		@Schema(description = "範本唯一識別名稱 (需對應資料庫範本設定)", example = "PR_IMPORT")
		@NotBlank(message = "範本名稱不可為空")
		@Size(max = 64, message = "範本名稱長度不可超過 64 字元")
		String name,

		@Schema(description = "範本種類代碼", example = "PR")
		@NotBlank(message = "範本種類代碼不可為空")
		@Size(max = 64, message = "範本種類代碼長度不可超過 64 字元")
		String type,

		@Schema(description = "檔案格式種類", example = "xlsx")
		@Size(max = 16, message = "檔案格式長度不可超過 16 字元")
		String fileType,

		@Schema(description = "檔案伺服器暫存路徑 (選填)", example = "/TEMPLATE/PR_IMPORT")
		@Size(max = 255, message = "暫存路徑長度不可超過 255 字元")
		String filePath,

		@Schema(description = "原始檔案名稱", example = "PR_IMPORT.xlsx")
		@Size(max = 255, message = "原始檔名長度不可超過 255 字元")
		String fileName
) {
}
