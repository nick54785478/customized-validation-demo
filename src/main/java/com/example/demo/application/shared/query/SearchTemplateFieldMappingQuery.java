package com.example.demo.application.shared.query;

import java.io.Serializable;

import com.example.demo.application.shared.vo.YesNo;

/**
 * 搜尋範本欄位對映查詢 (Search Template Field Mapping Query)
 * <p>
 * 遵循 {@code V + N + Query} 命名規範，以 Java 17 {@code record} 實作之不可變查詢條件載體。
 * 所有條件皆為可選條件 (Optional Filter Criteria)。
 * </p>
 *
 * @param templateName       範本名稱
 * @param templateSheetName 所屬工作表代碼
 * @param fieldCode          內部屬性代碼
 * @param displayName        實體標題名稱
 * @param activeFlag         啟用旗標
 */
public record SearchTemplateFieldMappingQuery(
		String templateName,
		String templateSheetName,
		String fieldCode,
		String displayName,
		YesNo activeFlag) implements Serializable {
}
