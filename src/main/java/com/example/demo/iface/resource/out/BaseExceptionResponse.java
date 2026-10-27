package com.example.demo.iface.resource.out;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 表現層單筆錯誤回應資源 (Base Exception Response Resource)
 * <p>
 * 遵循命名規範以 {@code N + Ved + Resource} / {@code Response} 定義。
 * 作為表現層向前端或客戶端回傳之單筆錯誤訊息 JSON 純資料載體。
 * </p>
 *
 * @param code    錯誤狀態代碼
 * @param message 錯誤提示訊息
 */
@Schema(
		name = "BaseExceptionResponse",
		description = "單筆錯誤回應資訊"
)
public record BaseExceptionResponse(
		@Schema(description = "錯誤狀態代碼", example = "PARAM_INVALID")
		String code,

		@Schema(description = "錯誤提示訊息", example = "上傳之 Excel 檔案不可為空")
		String message
) {
}
