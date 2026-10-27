package com.example.demo.application.shared.query;

import java.io.Serializable;

import com.example.demo.application.shared.vo.YesNo;

/**
 * 搜尋範本工作表對映查詢 (Search Template Sheet Mapping Query)
 * <p>
 * 遵循 {@code V + N + Query} 命名規範，以 Java 17 {@code record} 實作之不可變查詢條件載體。
 * 所有條件皆為可選條件 (Optional Filter Criteria)。
 * </p>
 *
 * @param templateName 範本名稱 (模糊/精準比對)
 * @param sheetCode    內部工作表代碼 (精準比對)
 * @param displayName  實體工作表名稱 (模糊比對)
 * @param activeFlag   啟用狀態 (YesNo.Y / YesNo.N)
 */
public record SearchTemplateSheetMappingQuery(
		String templateName,
		String sheetCode,
		String displayName,
		YesNo activeFlag) implements Serializable {
}
