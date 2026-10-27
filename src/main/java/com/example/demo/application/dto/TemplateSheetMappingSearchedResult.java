package com.example.demo.application.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 範本工作表對映搜尋結果清單資料載體 (Template Sheet Mapping Searched Result DTO)
 * <p>
 * 遵循 {@code N + Searched + Result} 命名規範，以 Java 17 {@code record} 實作純資料載體。
 * </p>
 *
 * @param items 符合搜尋條件之工作表對映清單
 * @param total 總筆數
 */
public record TemplateSheetMappingSearchedResult(
		List<TemplateSheetMappingGottenResult> items,
		long total) implements Serializable {
}
