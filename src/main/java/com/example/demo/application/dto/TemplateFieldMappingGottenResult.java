package com.example.demo.application.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.example.demo.application.shared.vo.YesNo;

/**
 * 範本欄位對映查詢結果資料載體 (Template Field Mapping Gotten Result DTO)
 * <p>
 * 遵循 {@code N + Gotten + Result} 命名規範，以 Java 17 {@code record} 實作純資料載體。
 * </p>
 *
 * @param id                 主鍵識別碼
 * @param templateName       範本名稱
 * @param templateSheetName 所屬工作表代碼
 * @param fieldCode          內部屬性代碼
 * @param displayName        實體標題名稱
 * @param columnIndex        預期欄位順序
 * @param isRequired         是否為必備欄位
 * @param dataType           資料型別
 * @param activeFlag         啟用旗標
 * @param createdTime        建立時間
 * @param updatedTime        更新時間
 */
public record TemplateFieldMappingGottenResult(
		Long id,
		String templateName,
		String templateSheetName,
		String fieldCode,
		String displayName,
		Integer columnIndex,
		YesNo isRequired,
		String dataType,
		YesNo activeFlag,
		LocalDateTime createdTime,
		LocalDateTime updatedTime) implements Serializable {
}
