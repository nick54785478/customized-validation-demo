package com.example.demo.iface.resource.out;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表現層多筆錯誤回應資源 (Base Exceptions Response Resource)
 * <p>
 * 遵循命名規範以 {@code N + Ved + Resource} / {@code Response} 定義。
 * 作為表現層向前端或客戶端回傳之批次錯誤清單 JSON 純資料載體。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseExceptionsResponse {

	/**
	 * 錯誤狀態代碼
	 */
	private String code;

	/**
	 * 詳細錯誤訊息清單
	 */
	private List<String> messages;

}
