package com.example.demo.application.shared.command;

import java.io.Serializable;

import com.example.demo.application.shared.vo.YesNo;

/**
 * 更新範本工作表對映指令 (Update Template Sheet Mapping Command)
 * <p>
 * 遵循 {@code V + N + Command} 命名規範，以 Java 17 {@code record} 實作之不可變業務指令載體。
 * </p>
 *
 * @param id           主鍵識別碼
 * @param templateName 範本唯一識別名稱
 * @param sheetCode    內部統一工作表代碼
 * @param displayName  Excel 實體工作表顯示文字
 * @param sheetIndex   預期工作表索引
 * @param isRequired   是否為必備工作表
 * @param activeFlag   啟用旗標
 */
public record UpdateTemplateSheetMappingCommand(
		Long id,
		String templateName,
		String sheetCode,
		String displayName,
		Integer sheetIndex,
		YesNo isRequired,
		YesNo activeFlag) implements Serializable {
}
