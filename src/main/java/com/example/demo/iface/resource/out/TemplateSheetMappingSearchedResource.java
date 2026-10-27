package com.example.demo.iface.resource.out;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 範本工作表對映搜尋結果清單回應資源 (Template Sheet Mapping Searched Resource)
 * <p>
 * 遵循 {@code N + Ved + Resource} 命名規範。
 * </p>
 *
 * @param items 符合條件之工作表對映清單
 * @param total 總筆數
 */
@Schema(
		name = "TemplateSheetMappingSearchedResource",
		description = "範本工作表對映搜尋結果清單回應資源"
)
public record TemplateSheetMappingSearchedResource(
		@Schema(description = "符合查詢條件之工作表對映項目清單")
		List<TemplateSheetMappingRetrievedResource> items,

		@Schema(description = "符合條件之資料總筆數", example = "5")
		long total
) {
}
