package com.example.demo.domain.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import com.example.demo.domain.policy.aggregate.ValidationPolicy;
import com.example.demo.domain.share.ContextRoot;
import com.example.demo.domain.share.TemplateLine;
import com.example.demo.share.bean.ValidateErrorProperty;

import lombok.extern.slf4j.Slf4j;

/**
 * 用來進行範本客製驗證的 Domain Service
 */
@Slf4j
@Service
public class ExcelValidateService {

	private static final ExpressionParser parser = new SpelExpressionParser();

	/**
	 * 主驗證方法，遍歷 Excel 資料並執行 SpEL 驗證。
	 * 
	 * @param contextRoot Context 資料
	 * @param policy      驗證原則
	 * @param vepList     錯誤訊息清單
	 * @return 錯誤訊息清單
	 */
	public List<ValidateErrorProperty> validateExcelData(ContextRoot contextRoot, List<ValidationPolicy> policyList,
			List<ValidateErrorProperty> vepList) {

		// 紀錄 Template Line
		Map<String, List<Map<String, String>>> sheet = contextRoot.getSheetMap();
		
		// 遍歷 SheetMap
		sheet.forEach((sheetName, dataSheet) -> {

			// 根據 SheetName 過濾出該 Policy 清單
			List<ValidationPolicy> filteredList = policyList.stream()
					.filter(e -> StringUtils.equals(sheetName, e.getTemplateSheetName())).collect(Collectors.toList());

			// 生成 TemplateMap Map<mappingFieldName, TemplateLine>
			Map<String, TemplateLine> templateMap = this.generateTemplateMap(filteredList);
			contextRoot.setSheet(dataSheet);
			for (int i = 0; i < dataSheet.size(); i++) {

				// 設置當前 row
				contextRoot.setCurrentRow(dataSheet.get(i));

				// 將 Policy 根據 MappingFieldName 轉換為 Map<MappingFieldName, policy>
				Map<String, ValidationPolicy> validateMap = filteredList.stream()
						.collect(Collectors.toMap(ValidationPolicy::getMappingFieldName, Function.identity()));

				// 遍歷當前 row (Map)
				for (Map.Entry<String, String> entry : contextRoot.getCurrentRow().entrySet()) {
					String k = entry.getKey(); // MapFieldName

					// 取得 Policy
					if (validateMap.containsKey(k)) {
						// 該 CellValue 的檢核規則
						ValidationPolicy policy = validateMap.get(k);
						// 取得 MappingFieldName
						String mappingFieldName = policy.getMappingFieldName();

						// 當欄位為空但毋需進行客製驗證的檢查時，跳過
						if (!StringUtils.equalsIgnoreCase(policy.getRule(), "ENFORCE_ROW_VALIDATION")
								&& isRowFieldBlank(contextRoot, mappingFieldName)) {
							continue;
						}

						// 設置當前處理的欄位值。
						this.setSingleMappingForRowCellValue(contextRoot, mappingFieldName);

						// 取得預處理表達式，可擴展替換特殊標記
						String expression = preProcessExpression(policy.getExpression());
						log.info("[validateExcelData] ROW after preprocessor: {}, {}", mappingFieldName, expression);

						Boolean expressionValue = evaluateExpression(expression, contextRoot);
						if (!Boolean.TRUE.equals(expressionValue)) {
							// 格式化驗證錯誤資訊，由於遍歷是從 0 開始，所以此處要輸入 i+1
							ValidateErrorProperty vep = formatRowValidateError(templateMap, policy, i + 1);
							vepList.add(vep);
						}
					}
				}
			}
		});
		return vepList;
	}

//	/**
//	 * 對整個 SHEET 的資料驗證
//	 */
//	public List<ValidateErrorProperty> validateExcelSheetData(SheetContextRoot contextRoot,
//			List<ValidationPolicy> policyList, List<ValidateErrorProperty> vepList) {
//		StandardEvaluationContext context = new StandardEvaluationContext();
//
//		// 紀錄 Template Line
//		List<Map<String, String>> dataSheet = contextRoot.getSheet();
//
//		// 生成 TemplateMap Map<mappingFieldName, TemplateLine>
//		Map<String, TemplateLine> templateMap = this.generateTemplateMap(policyList);
//
//		// 過濾出針對 SHEET 做驗證的 policy
//		Map<String, ValidationPolicy> validateMap = policyList.stream()
//				.collect(Collectors.toMap(ValidationPolicy::getMappingFieldName, Function.identity()));
//
//		return new ArrayList<>();
//	}

	/**
	 * 建立 TemplateMap
	 * 
	 * @param policyList
	 * @return Map<mappingFieldName, TemplateLine>
	 */
	private Map<String, TemplateLine> generateTemplateMap(List<ValidationPolicy> policyList) {
		Map<String, TemplateLine> templateMap = new HashMap<>();
		int columnIndex = 1;
		// 動態建立 MappingFieldName -> TemplateLine 的對應關係
		for (ValidationPolicy policy : policyList) {
			String mappingFieldName = policy.getMappingFieldName();
			templateMap.put(mappingFieldName,
					new TemplateLine(policy.getTemplateSheetName(), mappingFieldName, columnIndex));
			columnIndex++;
		}

		return templateMap;
	}

	/**
	 * 執行 SpEL 表達式並返回布林值。
	 * 
	 * @param expression
	 * @param contextRoot
	 * @return 結果
	 */
	private Boolean evaluateExpression(String expression, ContextRoot contextRoot) {
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setVariable("checkedValue", contextRoot.getCurrentCellValue());
		return parser.parseExpression(expression).getValue(context, Boolean.class);
	}

	/**
	 * 使用 SpEL 解析錯誤訊息。
	 *
	 * @param express      錯誤訊息模板 (可以包含 SpEL 表達式)
	 * @param excelAddress Excel 格式的欄位地址 (如 "B1")
	 * @param sheetName    Sheet Name
	 * @return 解析後的錯誤訊息
	 */
	private static String evaluateErrorMsg(String express, String excelAddress, String sheetName) {
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setVariable("excelAddress", excelAddress); // 設定變數
		context.setVariable("sheetName", sheetName); // SheetName
		return parser.parseExpression(express).getValue(context, String.class);
	}

	/**
	 * 判斷某欄位是否為空
	 * 
	 * @param contextRoot
	 * @param fieldName
	 * @return true/false
	 */
	private boolean isRowFieldBlank(ContextRoot contextRoot, String fieldName) {
		String value = contextRoot.getFieldValue(fieldName);
		return StringUtils.isBlank(value);
	}

	/**
	 * 設置當前處理的欄位值。
	 * 
	 * @param contextRoot
	 * @param fieldName   欄位名稱
	 */
	private void setSingleMappingForRowCellValue(ContextRoot contextRoot, String fieldName) {
		contextRoot.setCurrentCellValue(contextRoot.getFieldValue(fieldName));
	}

	/**
	 * 根據 MappingFieldName 取得對應的 TemplateLine
	 * 
	 * @param mappingFieldName 欄位名稱
	 * @return TemplateLine 對象
	 */
	public static TemplateLine getTemplateLine(Map<String, TemplateLine> templateLineMap, String mappingFieldName) {
		return templateLineMap.get(mappingFieldName);
	}

	/**
	 * 預處理表達式，可擴展替換特殊標記，視情況加料。
	 * 
	 * @param expression
	 * @return String
	 */
	private static String preProcessExpression(String expression) {
		// 可加強處理，例如替換特殊標記
		return expression;
	}

	/**
	 * 格式化驗證錯誤資訊。
	 * 
	 * @param templateLineMap
	 * @param policy
	 * @param rowIndex
	 */
	private static ValidateErrorProperty formatRowValidateError(Map<String, TemplateLine> templateLineMap,
			ValidationPolicy policy, int rowIndex) {
		// 取得 MappingFieldName 對應的 TemplateLine (包含 Excel Column Index)
		TemplateLine templateLine = getTemplateLine(templateLineMap, policy.getMappingFieldName());

		// 取得 Excel 地址，如 "B1"
		String excelAddress = templateLine.convertNumToAddress(rowIndex + 1, templateLine.getDataColumnNum());
		log.debug("rowIndex:{}, excelAddress:{}", rowIndex, excelAddress);
		// 使用 SpEL 解析錯誤訊息
		String errorMessage = evaluateErrorMsg(policy.getErrorMessage(), excelAddress, policy.getTemplateSheetName());
		return new ValidateErrorProperty(policy.getRule(), rowIndex, errorMessage);
	}

}
