package com.example.demo.infra.cv.normalizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.exception.ExcelValidationException;
import com.example.demo.application.shared.vo.ValidationError;
import com.example.demo.application.shared.vo.YesNo;
import com.example.demo.infra.persistence.entity.TemplateSheetMapping;

/**
 * 試算表工作表名稱正規化防腐器單元測試 (Template Sheet Normalizer Tests)
 */
class TemplateSheetNormalizerTest {

	private TemplateSheetNormalizer normalizer;

	@BeforeEach
	void setUp() {
		normalizer = new TemplateSheetNormalizer();
	}

	/**
	 * Feature: Excel 工作表頁籤名稱正規化轉換
	 * Scenario: 成功將試算表中客製中文頁籤名稱轉換為內部統一 SheetCode
	 * Given: 包含中文工作表「使用者名單」與「部門資料」的原始 Excel 工作表集合
	 * When: 調用 normalizer.normalize 執行第一層工作表防腐轉換
	 * Then: 轉換後的 Map Key 應對齊為內部代碼 "USER" 與 "DEPT"，資料列內容完整保留
	 */
	@Test
	@DisplayName("成功將客製中文工作表頁籤正規化為內部 SheetCode")
	void shouldNormalizeChineseSheetNamesToSheetCodes() {
		// Given
		String templateName = "USER_PROFILE";

		Map<String, List<Map<String, String>>> rawSheetMap = Map.of(
				"使用者名單", List.of(Map.of("姓名", "張三")),
				"部門資料", List.of(Map.of("部門", "資訊部"))
		);

		List<TemplateSheetMapping> mappings = List.of(
				new TemplateSheetMapping(templateName, "USER", "使用者名單", 0, YesNo.Y),
				new TemplateSheetMapping(templateName, "DEPT", "部門資料", 1, YesNo.Y)
		);

		// When
		Map<String, List<Map<String, String>>> result = normalizer.normalize(templateName, rawSheetMap, mappings);

		// Then
		assertNotNull(result);
		assertTrue(result.containsKey("USER"), "應包含正規化後的 USER 工作表");
		assertTrue(result.containsKey("DEPT"), "應包含正規化後的 DEPT 工作表");
		assertEquals("張三", result.get("USER").get(0).get("姓名"));
		assertEquals("資訊部", result.get("DEPT").get(0).get("部門"));
	}

	/**
	 * Feature: Excel 工作表前置防呆校驗
	 * Scenario: 偵測到缺少必要工作表時立即拋出結構化例外阻斷流程
	 * Given: 試算表中缺少標記為必備的「部門資料」工作表
	 * When: 調用 normalizer.normalize 進行前置工作表檢查
	 * Then: 應拋出 ExcelValidationException 且錯誤碼為 SHEET_VALIDATION_FAILED，錯誤清單包含缺少工作表之明確說明
	 */
	@Test
	@DisplayName("缺少必填工作表時拋出 SHEET_VALIDATION_FAILED 例外")
	void shouldThrowExceptionWhenRequiredSheetIsMissing() {
		// Given
		String templateName = "USER_PROFILE";

		// 試算表只有「使用者名單」，缺少必備的「部門資料」
		Map<String, List<Map<String, String>>> rawSheetMap = Map.of(
				"使用者名單", List.of(Map.of("姓名", "李四"))
		);

		List<TemplateSheetMapping> mappings = List.of(
				new TemplateSheetMapping(templateName, "USER", "使用者名單", YesNo.Y),
				new TemplateSheetMapping(templateName, "DEPT", "部門資料", YesNo.Y)
		);

		// When & Then
		ExcelValidationException exception = assertThrows(ExcelValidationException.class, () -> {
			normalizer.normalize(templateName, rawSheetMap, mappings);
		});

		assertEquals("SHEET_VALIDATION_FAILED", exception.getCode());
		List<ValidationError> errors = exception.getErrors();
		assertEquals(1, errors.size());
		assertTrue(errors.get(0).message().contains("部門資料"));
		assertTrue(errors.get(0).message().contains("DEPT"));
	}

	/**
	 * Feature: 工作表代碼直接匹配相容性
	 * Scenario: 當試算表頁籤已直接採用英文字段代碼 (如 USER, DEPT) 時亦可正確相容
	 * Given: 試算表工作表名稱直接為 "USER" 與 "DEPT"
	 * When: 調用 normalizer.normalize 進行轉換
	 * Then: 應能直接匹配通過校驗，產出以 "USER" 與 "DEPT" 為 Key 的資料集
	 */
	@Test
	@DisplayName("試算表已使用標準英文代碼時自動向下相容")
	void shouldSupportAlreadyStandardSheetCodes() {
		// Given
		String templateName = "USER_PROFILE";

		Map<String, List<Map<String, String>>> rawSheetMap = Map.of(
				"USER", List.of(Map.of("NAME", "王五")),
				"DEPT", List.of(Map.of("DEPT", "會計部"))
		);

		List<TemplateSheetMapping> mappings = List.of(
				new TemplateSheetMapping(templateName, "USER", "使用者名單", YesNo.Y),
				new TemplateSheetMapping(templateName, "DEPT", "部門資料", YesNo.Y)
		);

		// When
		Map<String, List<Map<String, String>>> result = normalizer.normalize(templateName, rawSheetMap, mappings);

		// Then
		assertNotNull(result);
		assertTrue(result.containsKey("USER"));
		assertTrue(result.containsKey("DEPT"));
		assertEquals("王五", result.get("USER").get(0).get("NAME"));
	}

	/**
	 * Feature: 未配置對映時之向下相容
	 * Scenario: 範本無任何工作表對映設定時原樣回傳原始資料集
	 * Given: mappings 為空集合
	 * When: 調用 normalizer.normalize
	 * Then: 回傳物件與傳入之 rawSheetMap 保持相同結構
	 */
	@Test
	@DisplayName("未設定工作表對映時原樣返回原始資料集")
	void shouldReturnRawMapWhenNoMappingsConfigured() {
		// Given
		String templateName = "LEGACY_TEMPLATE";
		Map<String, List<Map<String, String>>> rawSheetMap = Map.of(
				"SHEET1", List.of(Map.of("COL1", "VAL1"))
		);

		// When
		Map<String, List<Map<String, String>>> result = normalizer.normalize(templateName, rawSheetMap, Collections.emptyList());

		// Then
		assertEquals(rawSheetMap, result);
	}
}
