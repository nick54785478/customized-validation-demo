package com.example.demo.infra.cv.normalizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.exception.ExcelValidationException;
import com.example.demo.application.shared.vo.ValidationError;
import com.example.demo.application.shared.vo.YesNo;
import com.example.demo.infra.persistence.entity.TemplateFieldMapping;

/**
 * 試算表標題正規化防腐器單元測試 (Template Header Normalizer Tests)
 */
class TemplateHeaderNormalizerTest {

	private TemplateHeaderNormalizer normalizer;

	@BeforeEach
	void setUp() {
		normalizer = new TemplateHeaderNormalizer();
	}

	/**
	 * Feature: Excel 標題鍵值正規化轉換
	 * Scenario: 成功將試算表中客製中文標題轉換為內部統一 CamelCase 屬性代碼
	 * Given: 包含中文標題「姓名」、「年齡」、「身分證字號」的原始 Excel 工作表資料列
	 * When: 調用 normalizer.normalize 執行防腐轉換
	 * Then: 轉換後的資料 Map Key 應對齊為 "name", "age", "nationId" 且內容值保持不變
	 */
	@Test
	@DisplayName("成功將中文標題正規化為內部屬性鍵值")
	void shouldNormalizeChineseHeadersToFieldCodes() {
		// Given
		String templateName = "USER_PROFILE";
		String sheetName = "USER";

		Map<String, String> rawRow = new LinkedHashMap<>();
		rawRow.put("姓名", "張三");
		rawRow.put("年齡", "28");
		rawRow.put("身分證字號", "A123456789");
		Map<String, List<Map<String, String>>> rawSheetMap = Map.of(sheetName, List.of(rawRow));

		List<TemplateFieldMapping> mappings = List.of(
				new TemplateFieldMapping(templateName, sheetName, "name", "姓名", YesNo.Y),
				new TemplateFieldMapping(templateName, sheetName, "age", "年齡", YesNo.N),
				new TemplateFieldMapping(templateName, sheetName, "nationId", "身分證字號", YesNo.Y)
		);

		// When
		Map<String, List<Map<String, String>>> result = normalizer.normalize(templateName, rawSheetMap, mappings);

		// Then
		assertNotNull(result);
		List<Map<String, String>> normalizedRows = result.get(sheetName);
		assertEquals(1, normalizedRows.size());

		Map<String, String> normalizedRow = normalizedRows.get(0);
		assertEquals("張三", normalizedRow.get("name"));
		assertEquals("28", normalizedRow.get("age"));
		assertEquals("A123456789", normalizedRow.get("nationId"));
	}

	/**
	 * Feature: Excel 標題前置防呆校驗
	 * Scenario: 偵測到缺少必要欄位標題時立即拋出結構化例外
	 * Given: 試算表原始資料中缺少標記為必備的「身分證字號」標題欄位
	 * When: 調用 normalizer.normalize 進行前置標題檢查
	 * Then: 應拋出 ExcelValidationException 且錯誤清單包含缺少該欄位的明確錯誤說明
	 */
	@Test
	@DisplayName("缺少必填標題時拋出 HEADER_VALIDATION_FAILED 例外")
	void shouldThrowExceptionWhenRequiredHeaderIsMissing() {
		// Given
		String templateName = "USER_PROFILE";
		String sheetName = "USER";

		Map<String, String> rawRow = new LinkedHashMap<>();
		rawRow.put("姓名", "李四");
		rawRow.put("年齡", "30");
		// 故意缺少 "身分證字號"
		Map<String, List<Map<String, String>>> rawSheetMap = Map.of(sheetName, List.of(rawRow));

		List<TemplateFieldMapping> mappings = List.of(
				new TemplateFieldMapping(templateName, sheetName, "name", "姓名", YesNo.Y),
				new TemplateFieldMapping(templateName, sheetName, "nationId", "身分證字號", YesNo.Y)
		);

		// When & Then
		ExcelValidationException exception = assertThrows(ExcelValidationException.class, () -> {
			normalizer.normalize(templateName, rawSheetMap, mappings);
		});

		assertEquals("HEADER_VALIDATION_FAILED", exception.getCode());
		List<ValidationError> errors = exception.getErrors();
		assertEquals(1, errors.size());
		assertTrue(errors.get(0).message().contains("缺少必要欄位標題：【身分證字號】"));
	}

	/**
	 * Feature: 向下相容傳統原始標題模式
	 * Scenario: 當未設定任何欄位對映定義時保持原始資料結構回傳
	 * Given: 原始工作表資料與空的欄位對映清單
	 * When: 調用 normalizer.normalize
	 * Then: 資料應原樣回傳且不拋出任何異常
	 */
	@Test
	@DisplayName("無對映定義時保持向下相容原樣回傳")
	void shouldReturnRawMapWhenNoMappingsConfigured() {
		// Given
		String templateName = "LEGACY_TEMPLATE";
		Map<String, List<Map<String, String>>> rawSheetMap = Map.of(
				"SHEET1", List.of(Map.of("RAW_COL", "VAL1"))
		);

		// When
		Map<String, List<Map<String, String>>> result = normalizer.normalize(
				templateName, rawSheetMap, Collections.emptyList()
		);

		// Then
		assertEquals(rawSheetMap, result);
	}
}
