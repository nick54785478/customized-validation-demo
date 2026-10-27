package com.example.demo.iface.resource.out;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 範本工作表對映刪除成功回應資源 (Template Sheet Mapping Deleted Resource)
 * <p>
 * 遵循 {@code N + Ved + Resource} 命名規範。
 * </p>
 *
 * @param id      主鍵識別碼
 * @param message 刪除結果提示訊息
 */
@Schema(
		name = "TemplateSheetMappingDeletedResource",
		description = "範本工作表對映刪除成功回應資源"
)
public record TemplateSheetMappingDeletedResource(
		@Schema(description = "已刪除之工作表對映主鍵識別碼", example = "1")
		Long id,

		@Schema(description = "刪除結果提示訊息", example = "範本工作表對映刪除成功")
		String message
) {
}
