package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.application.exception.ExcelValidationException;
import com.example.demo.application.shared.vo.ValidationError;
import com.example.demo.application.shared.vo.YesNo;
import com.example.demo.infra.cv.normalizer.TemplateHeaderNormalizer;
import com.example.demo.infra.cv.normalizer.TemplateSheetNormalizer;
import com.example.demo.infra.cv.shared.context.ContextRoot;
import com.example.demo.infra.cv.validator.CustomValidator;
import com.example.demo.infra.persistence.entity.TemplateFieldMapping;
import com.example.demo.infra.persistence.entity.TemplateSheetMapping;
import com.example.demo.infra.persistence.entity.ValidationPolicy;
import com.example.demo.infra.util.ExcelUtil;

/**
 * PR Import (請購單批次匯入) 客製化驗證整合測試
 * <p>
 * <b>業務背景：</b><br>
 * 針對各業務/專案部門上傳之請購明細 Excel 試算表（包含物料料號、品名、規格、需求數量、預算單價、成本中心、需求交期），
 * 驗證雙層防腐層 (工作表與欄位名稱正規化) 與客製化業務規則 (SpEL 必填、型別、數值範圍、代碼清單合法性與重複性檢查)。
 * </p>
 */
@SpringBootTest
class PrImportValidateTest {

	@Autowired
	private CustomValidator customValidator;

	@Autowired
	private TemplateSheetNormalizer templateSheetNormalizer;

	@Autowired
	private TemplateHeaderNormalizer templateHeaderNormalizer;

	private static final String TEMPLATE_NAME = "PR_IMPORT";
	private static final String SHEET_CODE = "PR_ITEM";
	private static final String SHEET_DISPLAY_NAME = "請購明細清單";

	private List<TemplateSheetMapping> sheetMappings;
	private List<TemplateFieldMapping> fieldMappings;
	private List<ValidationPolicy> policies;

	@BeforeEach
	void setUp() {
		// 1. Level 1: 工作表層級防腐對映 (Sheet Mapping)
		sheetMappings = List.of(
				new TemplateSheetMapping(TEMPLATE_NAME, SHEET_CODE, SHEET_DISPLAY_NAME, 0, YesNo.Y)
		);

		// 2. Level 2: 欄位標題層級防腐對映 (Header Mapping)
		fieldMappings = List.of(
				new TemplateFieldMapping(TEMPLATE_NAME, SHEET_CODE, "itemNo", "物料料號", YesNo.Y),
				new TemplateFieldMapping(TEMPLATE_NAME, SHEET_CODE, "itemName", "物料品名", YesNo.Y),
				new TemplateFieldMapping(TEMPLATE_NAME, SHEET_CODE, "spec", "規格型號", YesNo.N),
				new TemplateFieldMapping(TEMPLATE_NAME, SHEET_CODE, "quantity", "需求數量", YesNo.Y),
				new TemplateFieldMapping(TEMPLATE_NAME, SHEET_CODE, "unitPrice", "預算單價", YesNo.Y),
				new TemplateFieldMapping(TEMPLATE_NAME, SHEET_CODE, "costCenter", "成本中心", YesNo.Y),
				new TemplateFieldMapping(TEMPLATE_NAME, SHEET_CODE, "requiredDate", "需求交期", YesNo.Y)
		);

		// 3. 客製驗證規則清單 (Validation Policies)
		policies = new ArrayList<>();

		// 3.1 SHEET 級別: 注入合法成本中心清單變數
		policies.add(new ValidationPolicy("SHEET", SHEET_CODE, "VARIABLE", "validCostCenters",
				"{'CC001', 'CC002', 'CC003', 'IT001', 'HR001'}", ""));

		// 3.2 SHEET 級別: 物料料號唯一性檢核 (整表不重複)
		policies.add(new ValidationPolicy("SHEET", SHEET_CODE, "VALIDATE_DUPLICATE", "itemNo",
				"#validateDuplicate(#sheet, 'itemNo')", ""));

		// 3.3 ROW 級別: 物料料號必填
		policies.add(new ValidationPolicy("ROW", SHEET_CODE, "ENFORCE_ROW_VALIDATION", "itemNo",
				"#checkedValue != null && #checkedValue.trim() != ''",
				"'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [物料料號] 為必填項目，不可為空'"));

		// 3.4 ROW 級別: 物料品名必填
		policies.add(new ValidationPolicy("ROW", SHEET_CODE, "ENFORCE_ROW_VALIDATION", "itemName",
				"#checkedValue != null && #checkedValue.trim() != ''",
				"'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [物料品名] 為必填項目，不可為空'"));

		// 3.5 ROW 級別: 需求數量必須為大於 0 的正整數
		policies.add(new ValidationPolicy("ROW", SHEET_CODE, "ENFORCE_ROW_VALIDATION", "quantity",
				"#isNumeric(#checkedValue) && T(java.lang.Integer).parseInt(#checkedValue) > 0",
				"'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [需求數量] 必須為大於 0 的正整數，當前值為 [' + #checkedValue + ']'"));

		// 3.6 ROW 級別: 預算單價必須為大於 0 的數值
		policies.add(new ValidationPolicy("ROW", SHEET_CODE, "ENFORCE_ROW_VALIDATION", "unitPrice",
				"#isNumeric(#checkedValue) && T(java.lang.Double).parseDouble(#checkedValue) > 0",
				"'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [預算單價] 必須為大於 0 的有效數值，當前值為 [' + #checkedValue + ']'"));

		// 3.7 ROW 級別: 成本中心代碼必須存在於核准清單中
		policies.add(new ValidationPolicy("ROW", SHEET_CODE, "ENFORCE_ROW_VALIDATION", "costCenter",
				"#validCostCenters.contains(#checkedValue)",
				"'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [成本中心] 代碼 [' + #checkedValue + '] 不存在或非有效清單'"));

		// 3.8 ROW 級別: 需求交期必須符合 YYYY-MM-DD 格式
		policies.add(new ValidationPolicy("ROW", SHEET_CODE, "ENFORCE_ROW_VALIDATION", "requiredDate",
				"#checkedValue != null && #checkedValue.matches('^\\d{4}-\\d{2}-\\d{2}$')",
				"'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [需求交期] 格式不合規，需為 YYYY-MM-DD (例如 2026-12-31)'"));
	}

	/**
	 * Feature: PR Import 請購單雙層防腐與客製驗證
	 * Scenario: 業務人員上傳繁體中文表頭之正確請購單 Excel 資料
	 * Given: 試算表頁籤為「請購明細清單」，欄位均為中文標題，且資料完全符合業務規則
	 * When: 經過 TemplateSheetNormalizer 與 TemplateHeaderNormalizer 轉換後交由 CustomValidator 驗證
	 * Then: 應順利通過雙層防腐正規化，且驗證結果錯誤清單為空 (無任何違規)
	 */
	@Test
	@DisplayName("場景 1：正確 PR 請購資料通過防腐層與所有客製驗證")
	void testPrImport_success() {
		// 1. 模擬業務人員上傳之繁體中文原始 Excel 資料
		Map<String, String> row1 = new LinkedHashMap<>();
		row1.put("物料料號", "MAT-001");
		row1.put("物料品名", "高效能伺服器");
		row1.put("規格型號", "64-Core / 256GB RAM");
		row1.put("需求數量", "5");
		row1.put("預算單價", "120000");
		row1.put("成本中心", "IT001");
		row1.put("需求交期", "2026-11-30");

		Map<String, String> row2 = new LinkedHashMap<>();
		row2.put("物料料號", "MAT-002");
		row2.put("物料品名", "人體工學辦公椅");
		row2.put("規格型號", "網布可調款");
		row2.put("需求數量", "20");
		row2.put("預算單價", "6500");
		row2.put("成本中心", "HR001");
		row2.put("需求交期", "2026-12-15");

		Map<String, List<Map<String, String>>> rawExcel = Map.of(SHEET_DISPLAY_NAME, List.of(row1, row2));

		// 2. 第一層防腐轉換：工作表「請購明細清單」 -> "PR_ITEM"
		Map<String, List<Map<String, String>>> sheetNormalized = templateSheetNormalizer
				.normalize(TEMPLATE_NAME, rawExcel, sheetMappings);

		// 3. 第二層防腐轉換：中文標題 -> 內部 CamelCase 屬性鍵值
		Map<String, List<Map<String, String>>> headerNormalized = templateHeaderNormalizer
				.normalize(TEMPLATE_NAME, sheetNormalized, fieldMappings);

		// 4. 構建 ContextRoot 執行驗證
		ContextRoot contextRoot = ContextRoot.builder()
				.params(new LinkedHashMap<>())
				.sheetMap(headerNormalized)
				.sheetDisplayNameMap(Map.of(SHEET_CODE, SHEET_DISPLAY_NAME))
				.build();

		List<ValidationError> errors = customValidator.validateExcelData(contextRoot, policies);

		// 5. 驗證全數通過
		assertNotNull(errors);
		assertTrue(errors.isEmpty(), "預期無錯誤，實際錯誤筆數: " + errors.size());
	}

	/**
	 * Feature: PR Import 前置防腐層校驗
	 * Scenario: 業務人員上傳之請購清單缺少必備欄位標題「預算單價」
	 * Given: 包含其他欄位但唯獨缺少必備標題「預算單價」
	 * When: 執行 TemplateHeaderNormalizer 正規化前置防呆
	 * Then: 應立即拋出 ExcelValidationException 且錯誤代碼為 HEADER_VALIDATION_FAILED
	 */
	@Test
	@DisplayName("場景 2：缺少必填標題「預算單價」時防腐層快速失敗攔截")
	void testPrImport_missingRequiredHeader_shouldThrowException() {
		// 模擬缺少「預算單價」
		Map<String, String> row = new LinkedHashMap<>();
		row.put("物料料號", "MAT-001");
		row.put("物料品名", "螢幕");
		row.put("需求數量", "10");
		row.put("成本中心", "IT001");
		row.put("需求交期", "2026-11-20");

		Map<String, List<Map<String, String>>> sheetNormalized = Map.of(SHEET_CODE, List.of(row));

		ExcelValidationException exception = assertThrows(ExcelValidationException.class, () ->
				templateHeaderNormalizer.normalize(TEMPLATE_NAME, sheetNormalized, fieldMappings));

		assertEquals("HEADER_VALIDATION_FAILED", exception.getCode());
		assertFalse(exception.getErrors().isEmpty());
		assertTrue(exception.getErrors().get(0).message().contains("預算單價"));
	}

	/**
	 * Feature: PR Import 業務規則檢核 (SpEL 規則引擎)
	 * Scenario: 資料列包含多個業務違規項目（數量為負數、成本中心不存在、交期格式錯誤）
	 * Given: 資料列中數量為 "-2"、成本中心為 "INVALID_CC"、需求交期為 "2026/11/15"
	 * When: 執行 CustomValidator 檢核
	 * Then: 應精確捕獲 3 筆驗證錯誤並指出違規欄位與錯誤座標
	 */
	@Test
	@DisplayName("場景 3：數量為負數、成本中心不存在、交期格式錯誤被正確攔截")
	void testPrImport_invalidRowValues_shouldDetectErrors() {
		Map<String, String> invalidRow = new LinkedHashMap<>();
		invalidRow.put("itemNo", "MAT-999");
		invalidRow.put("itemName", "伺服器硬碟");
		invalidRow.put("spec", "2TB NVMe");
		invalidRow.put("quantity", "-2");             // 違規：小於 0
		invalidRow.put("unitPrice", "5000");
		invalidRow.put("costCenter", "INVALID_CC");   // 違規：不存在於核准清單
		invalidRow.put("requiredDate", "2026/11/15"); // 違規：格式非 YYYY-MM-DD

		ContextRoot contextRoot = ContextRoot.builder()
				.params(new LinkedHashMap<>())
				.sheetMap(Map.of(SHEET_CODE, List.of(invalidRow)))
				.sheetDisplayNameMap(Map.of(SHEET_CODE, SHEET_DISPLAY_NAME))
				.build();

		List<ValidationError> errors = customValidator.validateExcelData(contextRoot, policies);

		assertEquals(3, errors.size());
		List<String> messages = errors.stream().map(ValidationError::message).toList();

		assertTrue(messages.stream().anyMatch(m -> m.contains("[需求數量] 必須為大於 0 的正整數")));
		assertTrue(messages.stream().anyMatch(m -> m.contains("[成本中心] 代碼 [INVALID_CC] 不存在")));
		assertTrue(messages.stream().anyMatch(m -> m.contains("[需求交期] 格式不合規")));
	}

	/**
	 * Feature: PR Import 整表級別唯一性檢核
	 * Scenario: 請購明細清單中出現重複的物料料號
	 * Given: 第一列與第二列的物料料號均填寫 "MAT-001"
	 * When: 執行 CustomValidator 檢核
	 * Then: validateDuplicate 規則應觸發並標記料號重複錯誤
	 */
	@Test
	@DisplayName("場景 4：物料料號重複時觸發 SHEET 級別唯一性錯誤")
	void testPrImport_duplicateItemNo_shouldDetectDuplicate() {
		Map<String, String> row1 = new LinkedHashMap<>();
		row1.put("itemNo", "MAT-001");
		row1.put("itemName", "滑鼠");
		row1.put("quantity", "10");
		row1.put("unitPrice", "300");
		row1.put("costCenter", "IT001");
		row1.put("requiredDate", "2026-11-20");

		Map<String, String> row2 = new LinkedHashMap<>();
		row2.put("itemNo", "MAT-001"); // 重複
		row2.put("itemName", "滑鼠替換包");
		row2.put("quantity", "5");
		row2.put("unitPrice", "300");
		row2.put("costCenter", "IT001");
		row2.put("requiredDate", "2026-11-25");

		ContextRoot contextRoot = ContextRoot.builder()
				.params(new LinkedHashMap<>())
				.sheetMap(Map.of(SHEET_CODE, List.of(row1, row2)))
				.sheetDisplayNameMap(Map.of(SHEET_CODE, SHEET_DISPLAY_NAME))
				.build();

		List<ValidationError> errors = customValidator.validateExcelData(contextRoot, policies);

		assertFalse(errors.isEmpty());
		assertTrue(errors.stream().anyMatch(e -> e.message().contains("重複") || e.message().contains("MAT-001")));
	}

	/**
	 * Feature: PR Import 實體 Excel 測試範本生成與全鏈路驗證
	 * Scenario: 生成包含刻意設計之業務違規資料的 PR_IMPORT.xlsx 範本並執行全鏈路雙層防腐與客製校驗
	 * Given: 包含正常筆（Row 2）與 5 處刻意設計的典型違規（數量負數、成本中心不存在、交期格式錯誤、單價為0、料號重複）
	 * When: 將 XSSFWorkbook 寫入 src/main/resources/templates/PR_IMPORT.xlsx 並透過 ExcelUtil 重新讀入驗證
	 * Then: 成功產生實體檔案，並精確偵測出 5 處業務檢核錯誤
	 */
	@Test
	@DisplayName("場景 5：生成 PR_IMPORT.xlsx 測試檔案至 templates 目錄並執行全鏈路驗證")
	void testGenerateAndValidatePrImportExcelFile() throws Exception {
		File templateFile = new File("src/main/resources/templates/PR_IMPORT.xlsx");
		templateFile.getParentFile().mkdirs();

		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			XSSFSheet sheet = workbook.createSheet(SHEET_DISPLAY_NAME);

			// 1. 建立繁體中文實體標題 (Row 1)
			XSSFRow headerRow = sheet.createRow(0);
			String[] headers = {"物料料號", "物料品名", "規格型號", "需求數量", "預算單價", "成本中心", "需求交期"};
			for (int i = 0; i < headers.length; i++) {
				XSSFCell cell = headerRow.createCell(i);
				cell.setCellValue(headers[i]);
			}

			// 2. 寫入資料列 (包含 1 筆正常資料 + 5 筆刻意違規資料)
			String[][] dataRows = {
					// Row 2: 正確基準資料
					{"MAT-1001", "高效能運算伺服器", "64-Core / 256GB RAM", "3", "150000", "IT001", "2026-11-30"},
					// Row 3: 錯誤 1 - 需求數量為負數 (需 > 0)
					{"MAT-1002", "人體工學升降桌", "雙馬達 160x80cm", "-5", "12000", "HR001", "2026-12-15"},
					// Row 4: 錯誤 2 - 成本中心代碼不存在
					{"MAT-1003", "彩色雷射印表機", "A4/A3 雙面彩色列印", "2", "35000", "SALES999", "2026-12-20"},
					// Row 5: 錯誤 3 - 需求交期格式非 YYYY-MM-DD
					{"MAT-1004", "網路交換器", "24-Port Gigabit PoE", "4", "18000", "CC001", "2026/11/25"},
					// Row 6: 錯誤 4 - 預算單價為 0 (需 > 0)
					{"MAT-1005", "無線會議麥克風", "一對二無線藍牙款", "1", "0", "CC002", "2026-12-31"},
					// Row 7: 錯誤 5 - 物料料號與 Row 2 重複
					{"MAT-1001", "高效能運算伺服器 (加購備品)", "同上規格", "2", "150000", "IT001", "2026-12-10"}
			};

			for (int r = 0; r < dataRows.length; r++) {
				XSSFRow row = sheet.createRow(r + 1);
				for (int c = 0; c < dataRows[r].length; c++) {
					XSSFCell cell = row.createCell(c);
					cell.setCellValue(dataRows[r][c]);
				}
			}

			try (FileOutputStream fos = new FileOutputStream(templateFile)) {
				workbook.write(fos);
			}
		}

		assertTrue(templateFile.exists(), "PR_IMPORT.xlsx 必須成功生成於 templates 目錄");
		assertTrue(templateFile.length() > 0, "檔案大小不可為 0");

		// 3. 模擬透過 ExcelUtil 讀取剛剛生成的實體檔案，執行全鏈路雙層防腐正規化與客製檢核
		try (FileInputStream fis = new FileInputStream(templateFile)) {
			Map<String, List<Map<String, String>>> rawExcelData = ExcelUtil.readAllSheets(fis);

			// Level 1: 工作表名稱正規化 (「請購明細清單」 -> "PR_ITEM")
			Map<String, List<Map<String, String>>> sheetNormalized = templateSheetNormalizer
					.normalize(TEMPLATE_NAME, rawExcelData, sheetMappings);

			// Level 2: 標題代碼正規化 (中文標題 -> CamelCase 鍵值)
			Map<String, List<Map<String, String>>> headerNormalized = templateHeaderNormalizer
					.normalize(TEMPLATE_NAME, sheetNormalized, fieldMappings);

			// 構建 ContextRoot 執行客製驗證
			ContextRoot contextRoot = ContextRoot.builder()
					.params(new LinkedHashMap<>())
					.sheetMap(headerNormalized)
					.sheetDisplayNameMap(Map.of(SHEET_CODE, SHEET_DISPLAY_NAME))
					.build();

			List<ValidationError> errors = customValidator.validateExcelData(contextRoot, policies);

			// 預期捕獲 6 個違規項目 (數量、成本中心、交期、單價各 1 筆，料號重複通報第 2 列與第 7 列共 2 筆)
			assertFalse(errors.isEmpty());
			assertEquals(6, errors.size(), "預期精確捕獲 6 處業務檢核錯誤");

			List<String> messages = errors.stream().map(ValidationError::message).toList();
			assertTrue(messages.stream().anyMatch(m -> m.contains("[需求數量] 必須為大於 0 的正整數")));
			assertTrue(messages.stream().anyMatch(m -> m.contains("[成本中心] 代碼 [SALES999] 不存在")));
			assertTrue(messages.stream().anyMatch(m -> m.contains("[需求交期] 格式不合規")));
			assertTrue(messages.stream().anyMatch(m -> m.contains("[預算單價] 必須為大於 0 的有效數值")));
			assertTrue(messages.stream().anyMatch(m -> m.contains("重複") && m.contains("MAT-1001")));
		}
	}
}
