package com.example.demo.iface.resource.in;

import com.example.demo.application.shared.vo.YesNo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 更新範本工作表對映請求資料載體 (Update Template Sheet Mapping Resource)
 * <p>
 * 遵循 {@code V + N + Resource} 命名規範。
 * 作為表現層輸入純資料載體 (Pure Data Carrier)。
 * </p>
 *
 * @param templateName 範本名稱 (例如: PR_IMPORT, USER_PROFILE)
 * @param sheetCode    內部統一工作表代碼 (例如: PR_ITEM, USER)
 * @param displayName  Excel 實體工作表顯示名稱 (例如: 請購明細清單, 使用者名單)
 * @param sheetIndex   預期工作表索引 (0-based，可為空)
 * @param isRequired   是否為必備工作表 (YesNo.Y / YesNo.N)
 * @param activeFlag   啟用旗標 (YesNo.Y / YesNo.N)
 */
@Schema(
		name = "UpdateTemplateSheetMappingResource",
		description = "更新範本工作表對映請求資源"
)
public record UpdateTemplateSheetMappingResource(
		@Schema(description = "範本唯一識別名稱 (需對應業務範本代碼)", example = "PR_IMPORT")
		@NotBlank(message = "範本名稱不可為空")
		@Size(max = 64, message = "範本名稱長度不可超過 64 字元")
		String templateName,

		@Schema(description = "內部統一工作表代碼 (作為資料層統一鍵值)", example = "PR_ITEM")
		@NotBlank(message = "工作表代碼不可為空")
		@Size(max = 64, message = "工作表代碼長度不可超過 64 字元")
		String sheetCode,

		@Schema(description = "Excel 試算表實體工作表標籤名稱", example = "請購明細清單")
		@NotBlank(message = "工作表顯示名稱不可為空")
		@Size(max = 128, message = "工作表顯示名稱長度不可超過 128 字元")
		String displayName,

		@Schema(description = "預期工作表索引 (0-based，若為空由系統名稱自動對齊)", example = "0")
		@PositiveOrZero(message = "預期工作表索引必須大於或等於 0")
		Integer sheetIndex,

		@Schema(description = "是否為必備工作表 (Y: 必備，缺失時中斷流程；N: 選填)", example = "Y")
		@NotNull(message = "是否為必備工作表註記不可為空")
		YesNo isRequired,

		@Schema(description = "啟用旗標 (Y: 啟用，N: 停用)", example = "Y")
		@NotNull(message = "啟用旗標註記不可為空")
		YesNo activeFlag
) {
}
