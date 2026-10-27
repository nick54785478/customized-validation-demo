package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.application.shared.vo.ValidationError;
import com.example.demo.infra.cv.shared.context.ContextRoot;
import com.example.demo.infra.cv.validator.CustomValidator;
import com.example.demo.infra.persistence.entity.ValidationPolicy;

@SpringBootTest
class ExcelValidateServiceTest {

	@Autowired
	private CustomValidator customValidator;

	@BeforeEach
	void setUp() throws Exception {
	}

	/**
	 * Feature: Excel 客製資料驗證引擎
	 * Scenario: 驗證試算表必填欄位時偵測到空值並產出驗證錯誤清單
	 * Given: 包含三筆使用者資料（其中第三筆性別為空）以及三條 ROW 級別強制非空驗證規則
	 * When: 調用 customValidator.validateExcelData 執行檢核
	 * Then: 應成功攔截到不符規則的資料列並回傳非空的 ValidationError 清單
	 */
	@Test
	void testValidateExcelData() {
		// 模擬 Excel 資料
		List<Map<String, String>> sheet = List.of(Map.of("name", "張三", "age", "24", "sex", "男"),
				Map.of("name", "李四", "age", "34", "sex", "男"), Map.of("name", "王五", "age", "28", "sex", ""));
		ContextRoot contextRoot = new ContextRoot();
		contextRoot.setSheet(sheet);
		contextRoot.setParams(new LinkedHashMap<>());

		Map<String, List<Map<String, String>>> sheetMap = Map.of("USER_PROFILE", sheet);

		List<ValidationPolicy> policies = List.of(
				new ValidationPolicy("ROW", "USER_PROFILE", "ENFORCE_ROW_VALIDATION", "name", "#checkedValue!=''",
						"'SheetName = ' + #sheetName + ', ' + #excelAddress + ' 資料檢核有誤，不能為空'"),
				new ValidationPolicy("ROW", "USER_PROFILE", "ENFORCE_ROW_VALIDATION", "age", "#checkedValue!=''",
						"'SheetName = ' + #sheetName + ', ' + #excelAddress + ' 資料檢核有誤，不能為空'"),
				new ValidationPolicy("ROW", "USER_PROFILE", "ENFORCE_ROW_VALIDATION", "sex", "#checkedValue!=''",
						"'SheetName = ' + #sheetName + ', ' + #excelAddress + ' 資料檢核有誤，不能為空'"));
		contextRoot.setSheetMap(sheetMap);

		List<ValidationError> validateExcelData = customValidator.validateExcelData(contextRoot, policies);

		assertTrue(!validateExcelData.isEmpty());

		System.out.println("validateExcelData: " + validateExcelData);
		System.out.println("validateExcelData size: " + validateExcelData.size());

	}

	/**
	 * Feature: Excel 客製資料驗證引擎
	 * Scenario: 當 ContextRoot 配置 sheetDisplayNameMap 時，錯誤訊息中的 #sheetName 應正確渲染為友善中文名稱
	 * Given: 工作表代碼為 USER，對應友善顯示名稱為「使用者名單」
	 * When: 調用 customValidator.validateExcelData 執行檢核
	 * Then: 產出的錯誤訊息中應包含「SheetName = 使用者名單」而非「SheetName = USER」
	 */
	@Test
	void testValidateExcelData_withSheetDisplayName_shouldRenderFriendlyName() {
		List<Map<String, String>> sheet = List.of(Map.of("name", "王五", "age", "28", "sex", ""));
		ContextRoot contextRoot = new ContextRoot();
		contextRoot.setSheet(sheet);
		contextRoot.setParams(new LinkedHashMap<>());
		contextRoot.setSheetDisplayNameMap(Map.of("USER", "使用者名單"));
		contextRoot.setSheetMap(Map.of("USER", sheet));

		List<ValidationPolicy> policies = List.of(
				new ValidationPolicy("ROW", "USER", "ENFORCE_ROW_VALIDATION", "sex", "#checkedValue!=''",
						"'SheetName = ' + #sheetName + ', ' + #excelAddress + ' 資料檢核有誤，不能為空'"));

		List<ValidationError> errors = customValidator.validateExcelData(contextRoot, policies);

		assertTrue(!errors.isEmpty());
		assertTrue(errors.get(0).message().contains("SheetName = 使用者名單"));
	}

}
