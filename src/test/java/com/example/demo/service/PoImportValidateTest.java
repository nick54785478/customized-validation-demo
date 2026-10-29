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
 * PO Import (採購訂單批次匯入) 客製化驗證整合測試
 * <p>
 * <b>業務背景：</b><br>
 * 針對各採購/供應鏈部門上傳之採購訂單 Excel 試算表（包含採購單號、項次、供應商代碼、物料料號、品名、採購數量、計量單位、採購單價、幣別、約定交期與備註），
 * 驗證雙層防腐層 (工作表與欄位名稱正規化) 與客製化業務規則 (SpEL 必填、型別、數值範圍、核准清單合法性與料號重複性檢查)。
 * </p>
 */
@SpringBootTest
class PoImportValidateTest {

	@Autowired
	private CustomValidator customValidator;

	@Autowired
	private TemplateSheetNormalizer templateSheetNormalizer;

	@Autowired
	private TemplateHeaderNormalizer templateHeaderNormalizer;

	private static final String TEMPLATE_NAME = "PO_IMPORT";
	private static final String SHEET_CODE = "PO_ITEM";
	private static final String SHEET_DISPLAY_NAME = "採購訂單明細";

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
				new TemplateFieldMapping(TEMPLATE_NAME, SHEET_CODE, "poNo", "採購單號", YesNo.Y),
				new TemplateFieldMapping(TEMPLATE_NAME, SHEET_CODE, "lineNo", "項次", YesNo.Y),
				new TemplateFieldMapping(TEMPLATE_NAME, SHEET_CODE, "vendorCode", "供應商代碼", YesNo.Y),
				new TemplateFieldMapping(TEMPLATE_NAME, SHEET_CODE, "itemNo", "物料料號", YesNo.Y),
				new TemplateFieldMapping(TEMPLATE_NAME, SHEET_CODE, "itemName", "物料品名", YesNo.Y),
				new TemplateFieldMapping(TEMPLATE_NAME, SHEET_CODE, "quantity", "採購數量", YesNo.Y),
				new TemplateFieldMapping(TEMPLATE_NAME, SHEET_CODE, "unit", "計量單位", YesNo.Y),
				new TemplateFieldMapping(TEMPLATE_NAME, SHEET_CODE, "unitPrice", "採購單價", YesNo.Y),
				new TemplateFieldMapping(TEMPLATE_NAME, SHEET_CODE, "currency", "幣別", YesNo.Y),
				new TemplateFieldMapping(TEMPLATE_NAME, SHEET_CODE, "deliveryDate", "約定交期", YesNo.Y),
				new TemplateFieldMapping(TEMPLATE_NAME, SHEET_CODE, "remark", "備註", YesNo.N)
		);

		// 3. 客製驗證規則清單 (Validation Policies)
		policies = new ArrayList<>();

		// 3.1 SHEET 級別: 注入核准供應商清單變數
		policies.add(new ValidationPolicy("SHEET", SHEET_CODE, "VARIABLE", "validVendors",
				"{'V1001', 'V1002', 'V1003', 'V2001', 'V8888'}", ""));

		// 3.2 SHEET 級別: 注入核准幣別清單變數
		policies.add(new ValidationPolicy("SHEET", SHEET_CODE, "VARIABLE", "validCurrencies",
				"{'TWD', 'USD', 'EUR', 'JPY', 'CNY'}", ""));

		// 3.3 SHEET 級別: 注入標準計量單位清單變數
		policies.add(new ValidationPolicy("SHEET", SHEET_CODE, "VARIABLE", "validUnits",
				"{'PC', 'KG', 'SET', 'BOX', 'M', 'EA'}", ""));

		// 3.4 SHEET 級別: 物料料號唯一性檢核 (整表不重複)
		policies.add(new ValidationPolicy("SHEET", SHEET_CODE, "VALIDATE_DUPLICATE", "itemNo",
				"#validateDuplicate(#sheet, 'itemNo')", ""));

		// 3.5 ROW 級別: 採購單號格式檢核
		policies.add(new ValidationPolicy("ROW", SHEET_CODE, "ENFORCE_ROW_VALIDATION", "poNo",
				"#checkedValue != null && #checkedValue.matches('^PO-\\d{4}-\\d{4}$')",
				"'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [採購單號] 格式不合規，需為 PO-YYYY-NNNN (例如 PO-2026-0001)'"));

		// 3.6 ROW 級別: 項次必須為大於 0 的正整數
		policies.add(new ValidationPolicy("ROW", SHEET_CODE, "ENFORCE_ROW_VALIDATION", "lineNo",
				"#isNumeric(#checkedValue) && T(java.lang.Integer).parseInt(#checkedValue) > 0",
				"'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [項次] 必須為大於 0 的正整數，當前值為 [' + #checkedValue + ']'"));

		// 3.7 ROW 級別: 供應商代碼必須存在於核准名單內
		policies.add(new ValidationPolicy("ROW", SHEET_CODE, "ENFORCE_ROW_VALIDATION", "vendorCode",
				"#validVendors.contains(#checkedValue)",
				"'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [供應商代碼] [' + #checkedValue + '] 不存在或非核准合格供應商'"));

		// 3.8 ROW 級別: 物料料號為必填項
		policies.add(new ValidationPolicy("ROW", SHEET_CODE, "ENFORCE_ROW_VALIDATION", "itemNo",
				"#checkedValue != null && #checkedValue.trim() != ''",
				"'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [物料料號] 為必填項目，不可為空'"));

		// 3.9 ROW 級別: 物料品名為必填項
		policies.add(new ValidationPolicy("ROW", SHEET_CODE, "ENFORCE_ROW_VALIDATION", "itemName",
				"#checkedValue != null && #checkedValue.trim() != ''",
				"'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [物料品名] 為必填項目，不可為空'"));

		// 3.10 ROW 級別: 採購數量必須為大於 0 的正整數
		policies.add(new ValidationPolicy("ROW", SHEET_CODE, "ENFORCE_ROW_VALIDATION", "quantity",
				"#isNumeric(#checkedValue) && T(java.lang.Integer).parseInt(#checkedValue) > 0",
				"'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [採購數量] 必須為大於 0 的正整數，當前值為 [' + #checkedValue + ']'"));

		// 3.11 ROW 級別: 計量單位必須在標準單位清單內
		policies.add(new ValidationPolicy("ROW", SHEET_CODE, "ENFORCE_ROW_VALIDATION", "unit",
				"#validUnits.contains(#checkedValue)",
				"'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [計量單位] [' + #checkedValue + '] 不合規，需為標準計量單位 (如 PC, KG, SET)'"));

		// 3.12 ROW 級別: 採購單價必須為大於 0 的有效數值
		policies.add(new ValidationPolicy("ROW", SHEET_CODE, "ENFORCE_ROW_VALIDATION", "unitPrice",
				"#isNumeric(#checkedValue) && T(java.lang.Double).parseDouble(#checkedValue) > 0",
				"'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [採購單價] 必須為大於 0 的有效數值，當前值為 [' + #checkedValue + ']'"));

		// 3.13 ROW 級別: 幣別必須在核准幣別清單內
		policies.add(new ValidationPolicy("ROW", SHEET_CODE, "ENFORCE_ROW_VALIDATION", "currency",
				"#validCurrencies.contains(#checkedValue)",
				"'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [幣別] [' + #checkedValue + '] 不合規，需為核准幣別 (如 TWD, USD, EUR)'"));

		// 3.14 ROW 級別: 約定交期必須符合 YYYY-MM-DD 格式
		policies.add(new ValidationPolicy("ROW", SHEET_CODE, "ENFORCE_ROW_VALIDATION", "deliveryDate",
				"#checkedValue != null && #checkedValue.matches('^\\d{4}-\\d{2}-\\d{2}$')",
				"'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [約定交期] 格式不合規，需為 YYYY-MM-DD (例如 2026-12-31)'"));
	}

	/**
	 * Feature: PO Import 採購訂單雙層防腐與客製校驗
	 * Scenario: 傳入完全合規之 PO Excel 資料
	 * Given: 包含標準工作表名稱「採購訂單明細」與所有合法資料列
	 * When: 執行雙層防腐正規化並套用 CustomValidator
	 * Then: 檢核結果清單為空，無任何違規錯誤
	 */
	@Test
	@DisplayName("場景 1：完全合規之採購訂單資料，雙層正規化與客製校驗應全數通過")
	void testValidPoImportData() {
		// 模擬從 Excel 讀取之原始資料 (工作表名稱為中文，欄位標題為中文)
		Map<String, String> row1 = new LinkedHashMap<>();
		row1.put("採購單號", "PO-2026-0001");
		row1.put("項次", "10");
		row1.put("供應商代碼", "V1001");
		row1.put("物料料號", "MAT-9001");
		row1.put("物料品名", "工業級乙太網路交換器");
		row1.put("採購數量", "5");
		row1.put("計量單位", "SET");
		row1.put("採購單價", "12500");
		row1.put("幣別", "TWD");
		row1.put("約定交期", "2026-12-15");
		row1.put("備註", "急件空運");

		Map<String, List<Map<String, String>>> rawExcelData = Map.of(SHEET_DISPLAY_NAME, List.of(row1));

		// Level 1: 工作表名稱正規化
		Map<String, List<Map<String, String>>> sheetNormalized = templateSheetNormalizer
				.normalize(TEMPLATE_NAME, rawExcelData, sheetMappings);
		assertTrue(sheetNormalized.containsKey(SHEET_CODE), "工作表應被正規化為系統代碼 PO_ITEM");

		// Level 2: 欄位標題代碼正規化
		Map<String, List<Map<String, String>>> headerNormalized = templateHeaderNormalizer
				.normalize(TEMPLATE_NAME, sheetNormalized, fieldMappings);

		// 驗證標題已轉換為 CamelCase
		Map<String, String> normalizedRow = headerNormalized.get(SHEET_CODE).get(0);
		assertEquals("PO-2026-0001", normalizedRow.get("poNo"));
		assertEquals("MAT-9001", normalizedRow.get("itemNo"));
		assertEquals("12500", normalizedRow.get("unitPrice"));

		// 執行客製規則檢核
		ContextRoot contextRoot = ContextRoot.builder()
				.params(new LinkedHashMap<>())
				.sheetMap(headerNormalized)
				.sheetDisplayNameMap(Map.of(SHEET_CODE, SHEET_DISPLAY_NAME))
				.build();

		List<ValidationError> errors = customValidator.validateExcelData(contextRoot, policies);
		assertTrue(errors.isEmpty(), "合規資料檢核錯誤清單應為空");
	}

	/**
	 * Feature: PO Import Level 1 工作表防腐層前置檢核
	 * Scenario: 缺少必備工作表「採購訂單明細」
	 * Given: Excel 檔案僅包含非相關之工作表名稱（如「Sheet1」）
	 * When: 執行 TemplateSheetNormalizer.normalize
	 * Then: 立即拋出 ExcelValidationException，說明缺少必備工作表
	 */
	@Test
	@DisplayName("場景 2：缺少必備工作表，Level 1 防腐層應即刻阻擋並拋出例外")
	void testMissingRequiredSheet() {
		Map<String, List<Map<String, String>>> rawExcelData = Map.of("OTHER_SHEET", List.of());

		ExcelValidationException exception = assertThrows(ExcelValidationException.class, () ->
				templateSheetNormalizer.normalize(TEMPLATE_NAME, rawExcelData, sheetMappings)
		);

		assertFalse(exception.getErrors().isEmpty());
		assertTrue(exception.getErrors().get(0).message().contains("缺少必要工作表"), "應提示缺少必要工作表");
	}

	/**
	 * Feature: PO Import Level 2 欄位標題防腐層前置檢核
	 * Scenario: 首列標題缺漏必備欄位「採購單價」
	 * Given: 包含「採購訂單明細」頁籤，但標題列未提供「採購單價」
	 * When: 執行 TemplateHeaderNormalizer.normalize
	 * Then: 在第 0 列攔截並拋出 ExcelValidationException，防止後續計算報錯
	 */
	@Test
	@DisplayName("場景 3：缺漏必備欄位標題，Level 2 防腐層應即刻阻擋並拋出例外")
	void testMissingRequiredHeaders() {
		Map<String, String> invalidHeaderRow = new LinkedHashMap<>();
		invalidHeaderRow.put("採購單號", "PO-2026-0001");
		invalidHeaderRow.put("項次", "10");
		invalidHeaderRow.put("供應商代碼", "V1001");
		invalidHeaderRow.put("物料料號", "MAT-9001");
		invalidHeaderRow.put("物料品名", "品名範例");
		invalidHeaderRow.put("採購數量", "5");
		invalidHeaderRow.put("計量單位", "SET");
		// 刻意缺少: "採購單價"
		invalidHeaderRow.put("幣別", "TWD");
		invalidHeaderRow.put("約定交期", "2026-12-15");

		Map<String, List<Map<String, String>>> sheetData = Map.of(SHEET_CODE, List.of(invalidHeaderRow));

		ExcelValidationException exception = assertThrows(ExcelValidationException.class, () ->
				templateHeaderNormalizer.normalize(TEMPLATE_NAME, sheetData, fieldMappings)
		);

		assertNotNull(exception.getErrors());
		assertTrue(exception.getErrors().stream().anyMatch(e -> e.message().contains("採購單價")), "應明確提示缺少【採購單價】標題");
	}

	/**
	 * Feature: PO Import 客製商業規則細部邊界校驗
	 * Scenario: 採購單資料包含各種典型業務違規
	 * Given: 採購單號格式錯誤、供應商不存在、數量小於等於0、單價非數值、幣別不合規、交期非有效格式
	 * When: 執行 CustomValidator.validateExcelData
	 * Then: 規則引擎準確抓出所有違規項目並給予精準之錯誤訊息
	 */
	@Test
	@DisplayName("場景 4：資料列各項違規資料檢核，規則引擎應精準捕捉所有錯誤")
	void testInvalidRowBusinessPolicies() {
		// Row 1: 單號格式錯誤 + 供應商代碼非法 + 數量為 0
		Map<String, String> row1 = new LinkedHashMap<>();
		row1.put("poNo", "INVALID-PO-999"); // 違規 1: 格式非 PO-YYYY-NNNN
		row1.put("lineNo", "10");
		row1.put("vendorCode", "V9999");    // 違規 2: 非核准供應商清單
		row1.put("itemNo", "MAT-8001");
		row1.put("itemName", "工業軸承");
		row1.put("quantity", "0");          // 違規 3: 數量必須 > 0
		row1.put("unit", "PC");
		row1.put("unitPrice", "350.0");
		row1.put("currency", "TWD");
		row1.put("deliveryDate", "2026-11-20");

		// Row 2: 單價為負數 + 幣別不支援 + 交期格式錯誤
		Map<String, String> row2 = new LinkedHashMap<>();
		row2.put("poNo", "PO-2026-0002");
		row2.put("lineNo", "-5");           // 違規 4: 項次必須為大於 0 之正整數
		row2.put("vendorCode", "V1002");
		row2.put("itemNo", "MAT-8002");
		row2.put("itemName", "高壓閥門");
		row2.put("quantity", "10");
		row2.put("unit", "UNKNOWN_UNIT");  // 違規 5: 計量單位不合規
		row2.put("unitPrice", "-50");       // 違規 6: 單價必須 > 0
		row2.put("currency", "GBP");        // 違規 7: 幣別非核准清單 (GBP 不在核准清單)
		row2.put("deliveryDate", "2026/11/20"); // 違規 8: 日期格式非 YYYY-MM-DD

		Map<String, List<Map<String, String>>> sheetMap = Map.of(SHEET_CODE, List.of(row1, row2));

		ContextRoot contextRoot = ContextRoot.builder()
				.params(new LinkedHashMap<>())
				.sheetMap(sheetMap)
				.sheetDisplayNameMap(Map.of(SHEET_CODE, SHEET_DISPLAY_NAME))
				.build();

		List<ValidationError> errors = customValidator.validateExcelData(contextRoot, policies);

		// 驗證至少捕獲上述多項明確之違規
		assertTrue(errors.size() >= 7, "應至少捕獲 7 項業務規則違規，實際捕獲: " + errors.size());
		assertTrue(errors.stream().anyMatch(e -> e.message().contains("採購單號")), "應包含採購單號格式錯誤");
		assertTrue(errors.stream().anyMatch(e -> e.message().contains("供應商代碼")), "應包含供應商代碼非法錯誤");
		assertTrue(errors.stream().anyMatch(e -> e.message().contains("採購數量")), "應包含採購數量為 0 錯誤");
		assertTrue(errors.stream().anyMatch(e -> e.message().contains("項次")), "應包含項次為負數錯誤");
		assertTrue(errors.stream().anyMatch(e -> e.message().contains("計量單位")), "應包含計量單位錯誤");
		assertTrue(errors.stream().anyMatch(e -> e.message().contains("採購單價")), "應包含採購單價負數錯誤");
		assertTrue(errors.stream().anyMatch(e -> e.message().contains("幣別")), "應包含幣別不支援錯誤");
		assertTrue(errors.stream().anyMatch(e -> e.message().contains("約定交期")), "應包含約定交期格式錯誤");
	}

	/**
	 * Feature: PO Import 實體 Excel 測試範本生成與全鏈路驗證
	 * Scenario: 生成包含刻意設計之業務違規資料的 PO_IMPORT.xlsx 範本並執行全鏈路雙層防腐與客製校驗
	 * Given: 包含正常筆與 5 處刻意設計的典型違規（數量負數、供應商不存在、交期格式錯誤、單價為0、料號重複）
	 * When: 將 XSSFWorkbook 寫入 src/main/resources/templates/PO_IMPORT.xlsx 並透過 ExcelUtil 重新讀入驗證
	 * Then: 成功產生實體檔案，並精確偵測出業務檢核錯誤
	 */
	@Test
	@DisplayName("場景 5：生成 PO_IMPORT.xlsx 測試檔案至 templates 目錄並執行全鏈路驗證")
	void testGenerateAndValidatePoImportExcelFile() throws Exception {
		File templateFile = new File("src/main/resources/templates/PO_IMPORT.xlsx");
		templateFile.getParentFile().mkdirs();

		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			XSSFSheet sheet = workbook.createSheet(SHEET_DISPLAY_NAME);

			// 1. 建立繁體中文實體標題 (Row 1)
			XSSFRow headerRow = sheet.createRow(0);
			String[] headers = {
					"採購單號", "項次", "供應商代碼", "物料料號", "物料品名",
					"採購數量", "計量單位", "採購單價", "幣別", "約定交期", "備註"
			};
			for (int i = 0; i < headers.length; i++) {
				XSSFCell cell = headerRow.createCell(i);
				cell.setCellValue(headers[i]);
			}

			// 2. 寫入資料列 (包含 1 筆正常資料 + 5 筆刻意違規資料)
			String[][] dataRows = {
					// Row 2: 正確基準資料
					{"PO-2026-0001", "10", "V1001", "MAT-9001", "工業伺服馬達", "5", "SET", "45000", "TWD", "2026-11-30", "生產線核心元件"},
					// Row 3: 錯誤 1 - 採購數量為負數 (需 > 0)
					{"PO-2026-0001", "20", "V1001", "MAT-9002", "變頻器控制器", "-2", "PC", "18000", "TWD", "2026-11-30", "備用件"},
					// Row 4: 錯誤 2 - 供應商代碼不存在
					{"PO-2026-0001", "30", "V9999", "MAT-9003", "感測傳導線纜", "10", "M", "500", "TWD", "2026-12-05", "非合格廠商"},
					// Row 5: 錯誤 3 - 約定交期格式非 YYYY-MM-DD
					{"PO-2026-0002", "10", "V1002", "MAT-9004", "可程式邏輯控制器", "3", "SET", "88000", "USD", "2026/12/10", "進口設備"},
					// Row 6: 錯誤 4 - 採購單價為 0 (需 > 0)
					{"PO-2026-0002", "20", "V1002", "MAT-9005", "安全急停開關", "8", "PC", "0", "USD", "2026-12-10", "報價異常"},
					// Row 7: 錯誤 5 - 物料料號與 Row 2 重複 (MAT-9001)
					{"PO-2026-0003", "10", "V2001", "MAT-9001", "工業伺服馬達 (重複)", "2", "SET", "45000", "TWD", "2026-12-15", "重複申購"}
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

		assertTrue(templateFile.exists(), "PO_IMPORT.xlsx 必須成功生成於 templates 目錄");
		assertTrue(templateFile.length() > 0, "檔案大小不可為 0");

		// 3. 模擬透過 ExcelUtil 讀取剛剛生成的實體檔案，執行全鏈路雙層防腐正規化與客製檢核
		try (FileInputStream fis = new FileInputStream(templateFile)) {
			Map<String, List<Map<String, String>>> rawExcelData = ExcelUtil.readAllSheets(fis);

			// Level 1: 工作表名稱正規化 (「採購訂單明細」 -> "PO_ITEM")
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

			// 驗證精確捕獲 6 處刻意設計的業務檢核錯誤 (數量、供應商、交期、單價各 1 筆，料號重複通報第 2 列與第 7 列共 2 筆)
			assertEquals(6, errors.size(), "應精確捕捉到 6 處預設業務違規");

			assertTrue(errors.stream().anyMatch(e -> e.message().contains("[採購數量] 必須為大於 0 的正整數")), "錯誤 1: 數量負數應被捕捉");
			assertTrue(errors.stream().anyMatch(e -> e.message().contains("[供應商代碼] [V9999] 不存在")), "錯誤 2: 供應商非法應被捕捉");
			assertTrue(errors.stream().anyMatch(e -> e.message().contains("[約定交期] 格式不合規")), "錯誤 3: 交期格式錯誤應被捕捉");
			assertTrue(errors.stream().anyMatch(e -> e.message().contains("[採購單價] 必須為大於 0 的有效數值")), "錯誤 4: 單價為 0 應被捕捉");
			assertTrue(errors.stream().anyMatch(e -> e.message().contains("重複") && e.message().contains("MAT-9001")), "錯誤 5: 料號 MAT-9001 重複應被捕捉");
		}
	}
}
