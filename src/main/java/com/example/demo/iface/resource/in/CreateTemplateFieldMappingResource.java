package com.example.demo.iface.resource.in;

import com.example.demo.application.shared.vo.YesNo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 建立範本欄位對映請求資料載體 (Create Template Field Mapping Resource)
 * <p>
 * 遵循 {@code V + N + Resource} 命名規範。
 * 作為表現層輸入純資料載體 (Pure Data Carrier)。
 * </p>
 *
 * @param templateName       範本名稱 (例如: PR_IMPORT, USER_PROFILE)
 * @param templateSheetName 所屬工作表代碼 (例如: PR_ITEM, USER)
 * @param fieldCode          內部屬性代碼 (例如: itemNo, age, nationId)
 * @param displayName        Excel 實體標題顯示名稱 (例如: 物料料號, 年齡, 身分證字號)
 * @param columnIndex        預期欄位順序 (1-based，可為空)
 * @param isRequired         是否為必備欄位 (YesNo.Y / YesNo.N，預設 N)
 * @param dataType           資料型別 (預設 "STRING")
 * @param activeFlag         啟用旗標 (YesNo.Y / YesNo.N，預設 Y)
 */
@Schema(
		name = "CreateTemplateFieldMappingResource",
		description = "建立範本欄位對映請求資源"
)
public record CreateTemplateFieldMappingResource(
		@Schema(description = "範本唯一識別名稱 (需對應工作表設定之 templateName)", example = "PR_IMPORT")
		@NotBlank(message = "範本名稱不可為空")
		@Size(max = 64, message = "範本名稱長度不可超過 64 字元")
		String templateName,

		@Schema(description = "所屬工作表代碼 (對應 template_sheet_mapping 之 sheetCode)", example = "PR_ITEM")
		@NotBlank(message = "所屬工作表代碼不可為空")
		@Size(max = 64, message = "所屬工作表代碼長度不可超過 64 字元")
		String templateSheetName,

		@Schema(description = "系統內部屬性鍵值代碼 (例如 itemNo, quantity)", example = "itemNo")
		@NotBlank(message = "內部屬性代碼不可為空")
		@Size(max = 64, message = "內部屬性代碼長度不可超過 64 字元")
		String fieldCode,

		@Schema(description = "Excel 試算表實體標題名稱 (例如 物料料號, 需求數量)", example = "物料料號")
		@NotBlank(message = "欄位顯示名稱不可為空")
		@Size(max = 128, message = "欄位顯示名稱長度不可超過 128 字元")
		String displayName,

		@Schema(description = "預期欄位順序 (1-based，若為空由系統自動對應)", example = "1")
		@Positive(message = "預期欄位順序必須大於 0")
		Integer columnIndex,

		@Schema(description = "是否為必備欄位 (Y: 必備，缺失時中斷流程；N: 選填，預設 N)", example = "Y")
		@NotNull(message = "是否為必備欄位註記不可為空")
		YesNo isRequired,

		@Schema(description = "資料欄位型別 (如 STRING, NUMERIC, DATE，預設 STRING)", example = "STRING")
		@NotBlank(message = "資料型別不可為空")
		@Size(max = 32, message = "資料型別長度不可超過 32 字元")
		String dataType,

		@Schema(description = "啟用旗標 (Y: 啟用，N: 停用，預設 Y)", example = "Y")
		@NotNull(message = "啟用旗標註記不可為空")
		YesNo activeFlag
) {
	/**
	 * 精簡建構子，提供預設值
	 */
	public CreateTemplateFieldMappingResource {
		if (isRequired == null) {
			isRequired = YesNo.N;
		}
		if (dataType == null || dataType.isBlank()) {
			dataType = "STRING";
		}
		if (activeFlag == null) {
			activeFlag = YesNo.Y;
		}
	}
}
