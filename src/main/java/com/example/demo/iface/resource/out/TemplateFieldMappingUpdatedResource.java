package com.example.demo.iface.resource.out;

import java.time.LocalDateTime;

import com.example.demo.application.shared.vo.YesNo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 範本欄位對映更新成功回應資源 (Template Field Mapping Updated Resource)
 * <p>
 * 遵循 {@code N + Ved + Resource} 命名規範。
 * </p>
 *
 * @param id                主鍵識別碼
 * @param templateName      範本名稱
 * @param templateSheetName 所屬工作表代碼
 * @param fieldCode         內部屬性代碼
 * @param displayName       欄位顯示名稱
 * @param columnIndex       欄位順序
 * @param isRequired        是否為必備欄位
 * @param dataType          資料型別
 * @param activeFlag        啟用旗標
 * @param createdTime       建立時間
 * @param updatedTime       更新時間
 */
@Schema(
		name = "TemplateFieldMappingUpdatedResource",
		description = "範本欄位對映更新成功回應資源"
)
public record TemplateFieldMappingUpdatedResource(
		@Schema(description = "主鍵識別碼", example = "1")
		Long id,

		@Schema(description = "範本唯一識別名稱", example = "PR_IMPORT")
		String templateName,

		@Schema(description = "所屬工作表代碼", example = "PR_ITEM")
		String templateSheetName,

		@Schema(description = "內部屬性代碼", example = "itemNo")
		String fieldCode,

		@Schema(description = "欄位顯示名稱", example = "物料料號")
		String displayName,

		@Schema(description = "欄位順序 (1-based)", example = "1")
		Integer columnIndex,

		@Schema(description = "是否為必備欄位 (Y/N)", example = "Y")
		YesNo isRequired,

		@Schema(description = "資料欄位型別", example = "STRING")
		String dataType,

		@Schema(description = "啟用旗標 (Y/N)", example = "Y")
		YesNo activeFlag,

		@Schema(description = "建立時間", example = "2026-10-28T00:00:00")
		LocalDateTime createdTime,

		@Schema(description = "更新時間", example = "2026-10-28T00:00:00")
		LocalDateTime updatedTime
) {
}
