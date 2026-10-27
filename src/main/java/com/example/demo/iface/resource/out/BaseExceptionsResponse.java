package com.example.demo.iface.resource.out;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 表現層多筆錯誤回應資源 (Base Exceptions Response Resource)
 * <p>
 * 遵循命名規範以 {@code N + Ved + Resource} / {@code Response} 定義。
 * 作為表現層向前端或客戶端回傳之批次錯誤清單 JSON 純資料載體。
 * </p>
 *
 * @param code     錯誤狀態代碼
 * @param messages 詳細錯誤訊息清單
 */
@Schema(
		name = "BaseExceptionsResponse",
		description = "多筆檢核/校驗錯誤回應資訊"
)
public record BaseExceptionsResponse(
		@Schema(description = "錯誤狀態代碼", example = "VALIDATE_FAILED")
		String code,

		@Schema(
				description = "詳細檢核錯誤訊息清單",
				example = "[\"SheetName : 請購明細清單, A2 資料檢核發生錯誤，欄位 itemNo 的值 'MAT-1001' 重複\", \"SheetName = 請購明細清單, D3 [需求數量] 必須為大於 0 的正整數，當前值為 [-5]\"]"
		)
		List<String> messages
) {
}
