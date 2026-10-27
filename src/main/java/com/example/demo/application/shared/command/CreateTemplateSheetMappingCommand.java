package com.example.demo.application.shared.command;

import java.io.Serializable;

import com.example.demo.application.shared.vo.YesNo;

/**
 * 建立範本工作表對映指令 (Create Template Sheet Mapping Command)
 * <p>
 * 遵循 {@code V + N + Command} 命名規範，以 Java 17 {@code record} 實作之不可變業務指令載體。
 * </p>
 *
 * @param templateName 範本唯一識別名稱 (例如: "USER_PROFILE")
 * @param sheetCode    內部統一工作表代碼 (例如: "USER")
 * @param displayName  Excel 實體工作表顯示文字 (例如: "使用者名單")
 * @param sheetIndex   預期工作表索引 (0-based，可為空)
 * @param isRequired   是否為必備工作表 (Y/N)
 * @param activeFlag   啟用旗標 (Y/N)
 */
public record CreateTemplateSheetMappingCommand(
		String templateName,
		String sheetCode,
		String displayName,
		Integer sheetIndex,
		YesNo isRequired,
		YesNo activeFlag) implements Serializable {
}
