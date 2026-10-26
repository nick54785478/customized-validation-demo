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

}
