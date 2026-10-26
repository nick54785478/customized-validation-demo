package com.example.demo.iface.resource.out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 範本上傳回應資料載體 (Template Uploaded Response Resource)
 * <p>
 * 遵循命名規範以 {@code N + Ved + Resource} 命名。
 * 作為表現層輸出純資料載體 (Pure Data Carrier)，封裝 API 回應狀態與訊息。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateUploadedResource {

	/**
	 * 回應狀態碼 (例如: "200")
	 */
	private String code;

	/**
	 * 回應提示訊息 (例如: "上傳成功")
	 */
	private String message;

}
