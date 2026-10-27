package com.example.demo.iface.resource.out;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 範本上傳回應資料載體 (Template Uploaded Response Resource)
 * <p>
 * 遵循命名規範以 {@code N + Ved + Resource} 命名。
 * 作為表現層輸出純資料載體 (Pure Data Carrier)，封裝 API 回應狀態與訊息。
 * </p>
 *
 * @param code    回應狀態碼 (例如: "200")
 * @param message 回應提示訊息 (例如: "上傳成功")
 */
@Schema(
		name = "TemplateUploadedResource",
		description = "範本上傳校驗成功回應資源"
)
public record TemplateUploadedResource(
		@Schema(description = "回應狀態代碼", example = "200")
		String code,

		@Schema(description = "回應結果訊息", example = "上傳成功")
		String message
) {
}
