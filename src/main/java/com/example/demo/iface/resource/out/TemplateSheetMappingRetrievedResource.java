package com.example.demo.iface.resource.out;

import java.time.LocalDateTime;

import com.example.demo.application.shared.vo.YesNo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 範本工作表對映查詢結果回應資源 (Template Sheet Mapping Retrieved Resource)
 * <p>
 * 遵循 {@code N + Ved + Resource} 命名規範。
 * </p>
 *
 * @param id           主鍵識別碼
 * @param templateName 範本名稱
 * @param sheetCode    工作表代碼
 * @param displayName  工作表顯示名稱
 * @param sheetIndex   預期工作表索引
 * @param isRequired   是否為必備工作表
 * @param activeFlag   啟用旗標
 * @param createdTime  建立時間
 * @param updatedTime  更新時間
 */
@Schema(
		name = "TemplateSheetMappingRetrievedResource",
		description = "範本工作表對映查詢結果回應資源"
)
public record TemplateSheetMappingRetrievedResource(
		@Schema(description = "主鍵識別碼", example = "1")
		Long id,

		@Schema(description = "範本唯一識別名稱", example = "PR_IMPORT")
		String templateName,

		@Schema(description = "內部工作表代碼", example = "PR_ITEM")
		String sheetCode,

		@Schema(description = "Excel 實體工作表顯示名稱", example = "請購明細清單")
		String displayName,

		@Schema(description = "預期工作表索引 (0-based)", example = "0")
		Integer sheetIndex,

		@Schema(description = "是否為必備工作表 (Y/N)", example = "Y")
		YesNo isRequired,

		@Schema(description = "啟用旗標 (Y/N)", example = "Y")
		YesNo activeFlag,

		@Schema(description = "建立時間", example = "2026-10-28T00:00:00")
		LocalDateTime createdTime,

		@Schema(description = "更新時間", example = "2026-10-28T00:00:00")
		LocalDateTime updatedTime
) {
}
