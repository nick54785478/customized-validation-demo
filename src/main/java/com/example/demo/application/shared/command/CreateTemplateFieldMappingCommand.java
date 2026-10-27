package com.example.demo.application.shared.command;

import java.io.Serializable;

import com.example.demo.application.shared.vo.YesNo;

/**
 * 建立範本欄位對映指令 (Create Template Field Mapping Command)
 * <p>
 * 遵循 {@code V + N + Command} 命名規範，以 Java 17 {@code record} 實作之不可變業務指令載體。
 * </p>
 *
 * @param templateName       範本唯一識別名稱 (例如: "USER_PROFILE")
 * @param templateSheetName 所屬工作表代碼 (例如: "USER")
 * @param fieldCode          內部統一屬性代碼 (例如: "NAME", "nationId")
 * @param displayName        Excel 實體標題顯示文字 (例如: "姓名", "身分證字號")
 * @param columnIndex        預期欄位順序 (1-based，可為空)
 * @param isRequired         是否為必填/必備欄位 (Y/N)
 * @param dataType           資料型別 (STRING, NUMBER, DATE...)
 * @param activeFlag         啟用旗標 (Y/N)
 */
public record CreateTemplateFieldMappingCommand(
		String templateName,
		String templateSheetName,
		String fieldCode,
		String displayName,
		Integer columnIndex,
		YesNo isRequired,
		String dataType,
		YesNo activeFlag) implements Serializable {
}
