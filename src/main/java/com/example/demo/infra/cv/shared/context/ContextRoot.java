package com.example.demo.infra.cv.shared.context;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Excel 上下文根資料
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextRoot {

	private Map<String, String> currentRow; // 當前處理的 row

	private String currentCellValue; // 當前驗證值

	private Map<String, List<Map<String, String>>> sheetMap; // 所有 Sheet 資料 Map<sheetName, sheet>

	private List<Map<String, String>> sheet; // 所有 Sheet 資料( Excel 轉換的 List<Map>)

	@Builder.Default
	private Map<String, Object> params = new java.util.LinkedHashMap<>(); // ParamMap 參數設置

	@Builder.Default
	private Map<String, String> sheetDisplayNameMap = new java.util.LinkedHashMap<>(); // 工作表代碼與外部顯示名稱之對照字典 Map<sheetCode, displayName>

	/**
	 * 透過 FieldName 取得該 FieldValue
	 * 
	 * @param fieldName
	 * @return String
	 */
	public String getFieldValue(String fieldName) {
		return currentRow.get(fieldName);
	}

	/**
	 * 依據工作表內部代碼取得對應之外部顯示名稱（若無對照則回退為內部代碼）
	 *
	 * @param sheetCode 內部標準工作表代碼 (例如: "USER")
	 * @return 外部顯示名稱 (例如: "使用者名單")，若不存在則回傳 sheetCode 本身
	 */
	public String getSheetDisplayName(String sheetCode) {
		if (sheetDisplayNameMap != null && sheetDisplayNameMap.containsKey(sheetCode)) {
			return sheetDisplayNameMap.get(sheetCode);
		}
		return sheetCode;
	}

}
