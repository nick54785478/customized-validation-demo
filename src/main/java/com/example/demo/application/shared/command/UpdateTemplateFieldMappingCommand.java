package com.example.demo.application.shared.command;

import java.io.Serializable;

import com.example.demo.application.shared.vo.YesNo;

/**
 * 更新範本欄位對映指令 (Update Template Field Mapping Command)
 * <p>
 * 遵循 {@code V + N + Command} 命名規範，以 Java 17 {@code record} 實作之不可變業務指令載體。
 * </p>
 *
 * @param id                 主鍵識別碼
 * @param templateName       範本唯一識別名稱
 * @param templateSheetName 所屬工作表代碼
 * @param fieldCode          內部統一屬性代碼
 * @param displayName        Excel 實體標題顯示文字
 * @param columnIndex        預期欄位順序
 * @param isRequired         是否為必備欄位
 * @param dataType           資料型別
 * @param activeFlag         啟用旗標
 */
public record UpdateTemplateFieldMappingCommand(
		Long id,
		String templateName,
		String templateSheetName,
		String fieldCode,
		String displayName,
		Integer columnIndex,
		YesNo isRequired,
		String dataType,
		YesNo activeFlag) implements Serializable {
}
