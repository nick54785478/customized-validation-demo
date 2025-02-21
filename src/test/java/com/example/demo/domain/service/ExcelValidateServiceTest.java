package com.example.demo.domain.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.domain.policy.aggregate.ValidationPolicy;
import com.example.demo.domain.share.ContextRoot;
import com.example.demo.share.bean.ValidateErrorProperty;

@SpringBootTest
class ExcelValidateServiceTest {

	@Autowired
	private ExcelValidateService excelValidateService;

	@BeforeEach
	void setUp() throws Exception {
	}

	@Test
	void testValidateExcelData() {
		// 模擬 Excel 資料
		List<Map<String, String>> sheet = List.of(Map.of("name", "張三", "age", "24", "sex", "男"),
				Map.of("name", "李四", "age", "34", "sex", "男"), Map.of("name", "王五", "age", "28", "sex", ""));
		ContextRoot contextRoot = new ContextRoot();
		contextRoot.setSheet(sheet);

		List<ValidationPolicy> policies = List.of(
				new ValidationPolicy("ROW", "ENFORCE_ROW_VALIDATION", "name", "#checkedValue!=''"),
				new ValidationPolicy("ROW", "ENFORCE_ROW_VALIDATION", "age", "#checkedValue!=''"),
				new ValidationPolicy("ROW", "ENFORCE_ROW_VALIDATION", "sex", "#checkedValue!=''"));

		List<ValidateErrorProperty> validateExcelData = excelValidateService.validateExcelData(contextRoot, policies,
				new ArrayList<>());

		assertTrue(!validateExcelData.isEmpty());

		System.out.println("validateExcelData: " + validateExcelData);
		System.out.println("validateExcelData size: " + validateExcelData.size());

	}

}
