package com.example.demo.iface.resource.out;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 範本欄位對映搜尋結果清單回應資源 (Template Field Mapping Searched Resource)
 * <p>
 * 遵循 {@code N + Ved + Resource} 命名規範。
 * </p>
 *
 * @param items 符合條件之欄位對映清單
 * @param total 總筆數
 */
@Schema(
		name = "TemplateFieldMappingSearchedResource",
		description = "範本欄位對映搜尋結果清單回應資源"
)
public record TemplateFieldMappingSearchedResource(
		@Schema(description = "符合查詢條件之欄位對映清單")
		List<TemplateFieldMappingRetrievedResource> items,

		@Schema(description = "符合條件之資料總筆數", example = "10")
		long total
) {
}
