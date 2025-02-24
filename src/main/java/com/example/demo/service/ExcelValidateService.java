package com.example.demo.service;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.example.demo.util.ExcelAddressParser;
import com.example.demo.util.ValidationUtil;
import com.example.demo.util.VariableUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 用來進行範本客製驗證的 Application Service
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
		Map<String, List<Map<String, String>>> sheetMap = contextRoot.getSheetMap();

		// 根據 SheetName 進行 policy 分群
		Map<String, List<ValidationPolicy>> policyMap = policyList.stream()
				.collect(Collectors.groupingBy(ValidationPolicy::getTemplateSheetName));

		// 遍歷 SheetMap
		sheetMap.forEach((sheetName, dataSheet) -> {
			// 取得所有 MappingFieldName List
			Set<String> mappingFieldNames = dataSheet.stream().flatMap(map -> map.keySet().stream()) // 提取所有 Key
					.collect(Collectors.toCollection(LinkedHashSet::new)); // 使用 Set 避免重複

			// 生成 TemplateMap Map<mappingFieldName, TemplateLine>
			Map<String, TemplateLine> templateMap = this.generateTemplateMap(sheetName, mappingFieldNames);

			// 設置當前的 Sheet
			contextRoot.setSheet(dataSheet);
			// 初始化 SpEL 上下文，準備變數與方法。
			StandardEvaluationContext context = initContext(contextRoot);
			if (policyMap.containsKey(sheetName)) {
				List<ValidationPolicy> policies = policyMap.get(sheetName);

				// 根據 Policy 去驗證資料
				policies.stream().forEach(policy -> {
					// 處理 ROW 資料
					if (StringUtils.equalsIgnoreCase(policy.getType(), "ROW")) {
						// 遍歷當前的 Sheet 資料
						for (int i = 0; i < dataSheet.size(); i++) {
							contextRoot.setCurrentRow(dataSheet.get(i)); // 設置當前 row
							String mappingFieldName = StringUtils.trimToEmpty(policy.getMappingFieldName());
							if (!StringUtils.equalsIgnoreCase(policy.getRule(), "ENFORCE_ROW_VALIDATION")
									&& isRowFieldBlank(contextRoot, mappingFieldName)) {
								continue;
							}
							// 設置當前處理的欄位值。
							this.setSingleMappingForRowCellValue(contextRoot, mappingFieldName);
							// 取得預處理表達式，可擴展替換特殊標記
							String expression = preProcessExpression(policy.getExpression());
							log.debug("[validateExcelData] ROW after preprocessor: {}, {}", mappingFieldName,
									expression);

							// 執行 Expression
							Boolean expressionValue = evaluateExpression(context, expression, contextRoot);
							if (!Boolean.TRUE.equals(expressionValue)) {
								// 格式化驗證錯誤資訊，由於遍歷是從 0 開始，所以此處要輸入 i+1
								ValidateErrorProperty vep = formatRowValidateError(context, templateMap, policy, i + 1);
								vepList.add(vep);
							}
						}
						// 處理 SHEET 資料
					} else if (StringUtils.equalsIgnoreCase(policy.getType(), "SHEET")) {

						if (StringUtils.equalsIgnoreCase("VARIABLE", policy.getRule())) {
							// 如果是 VARIABLE => 進行變數設置
							Object obj = parser.parseExpression(policy.getExpression()).getValue(context);
							contextRoot.getParams().put(policy.getMappingFieldName(), obj);
							context.setVariable(policy.getMappingFieldName(), obj);
						} else {
							// 處理 SHEET 的 Policy
							// 取得預處理表達式，可擴展替換特殊標記
							String expression = preProcessExpression(policy.getExpression());
							// 執行 Expression，並回傳 Map< rowIndex, errorMessage >
							Map<Integer, String> expressionValue = (Map<Integer, String>) parser
									.parseExpression(expression).getValue(context);

							if (expressionValue != null) {
								TemplateLine templateLine = getTemplateLine(templateMap, policy.getMappingFieldName());
								expressionValue.forEach((dataRowIndex, errorMessage) -> {
									// 取得 ExcelAddress，這邊需 +1 (要含標題的 row)，因為是 1-based
									String excelAddress = ExcelAddressParser.convertNumToAddress(dataRowIndex + 1,
											templateLine.getDataColumnNum());
									log.debug("dataRowIndex:{}, errorMessage:{}", dataRowIndex, errorMessage);
									ValidateErrorProperty vep = new ValidateErrorProperty();
									vep.setMessage("SheetName : " + sheetName + ", " + excelAddress + " 資料檢核發生錯誤，"
											+ errorMessage);
									vepList.add(vep);
								});
							}
							log.debug("expressionValue : {}", expressionValue);
						}
					}
				});
			}
		});
		return vepList;
	}

	/**
	 * 建立 TemplateMap
	 * 
	 * @param sheetName         SheetName
	 * @param mappingFieldNames MappingFieldName 清單
	 * @return Map<mappingFieldName, TemplateLine>
	 */
	private Map<String, TemplateLine> generateTemplateMap(String sheetName, Set<String> mappingFieldNames) {
		Map<String, TemplateLine> templateMap = new HashMap<>();
		int columnIndex = 1;
		for (String mappingFieldName : mappingFieldNames) {
			templateMap.put(mappingFieldName, new TemplateLine(sheetName, mappingFieldName, columnIndex));
			columnIndex++;
		}
		return templateMap;
	}

	/**
	 * 執行 SpEL 表達式並返回布林值。
	 * 
	 * @param context
	 * @param expression
	 * @param contextRoot
	 * @return 結果
	 */
	private Boolean evaluateExpression(StandardEvaluationContext context, String expression, ContextRoot contextRoot) {
		context.setVariable("checkedValue", contextRoot.getCurrentCellValue());
		return parser.parseExpression(expression).getValue(context, Boolean.class);
	}

	/**
	 * 使用 SpEL 解析錯誤訊息。
	 *
	 * @param context      Context
	 * @param express      錯誤訊息模板 (可以包含 SpEL 表達式)
	 * @param excelAddress Excel 格式的欄位地址 (如 "B1")
	 * @param sheetName    Sheet Name
	 * @return 解析後的錯誤訊息
	 */
	private static String evaluateErrorMsg(StandardEvaluationContext context, String express, String excelAddress,
			String sheetName) {
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
		// 可加強處理，例如: 替換特殊標記
		return expression;
	}

	/**
	 * 格式化驗證錯誤資訊。
	 * 
	 * @param context
	 * @param templateLineMap
	 * @param policy
	 * @param rowIndex
	 * @return ValidateErrorProperty
	 */
	private static ValidateErrorProperty formatRowValidateError(StandardEvaluationContext context,
			Map<String, TemplateLine> templateLineMap, ValidationPolicy policy, int rowIndex) {
		// 取得 MappingFieldName 對應的 TemplateLine (包含 Excel Column Index)
		TemplateLine templateLine = getTemplateLine(templateLineMap,
				StringUtils.trimToEmpty(policy.getMappingFieldName()));
		// 取得 Excel 地址，如 "B1"，此處要 +1 (要含標題的 row)
		String excelAddress = ExcelAddressParser.convertNumToAddress(rowIndex + 1, templateLine.getDataColumnNum());
		log.debug("rowIndex:{}, excelAddress:{}", rowIndex, excelAddress);
		// 使用 SpEL 解析錯誤訊息
		String errorMessage = evaluateErrorMsg(context, policy.getErrorMessage(), excelAddress,
				policy.getTemplateSheetName());
		return new ValidateErrorProperty(policy.getRule(), rowIndex, errorMessage);
	}

	/**
	 * 初始化 SpEL 上下文，準備變數與方法。
	 * 
	 * @param contextRoot
	 * @return StandardEvaluationContext
	 */
	public StandardEvaluationContext initContext(ContextRoot contextRoot) {
		StandardEvaluationContext context = new StandardEvaluationContext();
		// 準備基礎 Param 資料
		Map<String, Object> params = contextRoot.getParams();
		params.put("sheet", contextRoot.getSheet());
		contextRoot.setParams(params);
		this.setContextVariable(contextRoot, context);
		methodRegistration(context);
		return context;
	}

	/**
	 * 將 contextRoot 內的參數 (params) 設置為 SpEL 上下文變數，讓驗證時可以動態讀取參數值。
	 * 
	 * @param contextRoot
	 * @param context
	 */
	private void setContextVariable(ContextRoot contextRoot, StandardEvaluationContext context) {
		// 將 contextRoot 內的參數 (params) 設置為 SpEL 上下文變數，讓驗證時可以動態讀取參數值。
		for (Map.Entry<String, Object> p : contextRoot.getParams().entrySet()) {
			// minValue
			context.setVariable(p.getKey(), p.getValue());
		}
		// 用法範例:
		// #minValue < #checkedValue && #checkedValue < #maxValue
	}

	/**
	 * 將 ValidationUtils 和 VariableUtil 內的所有方法註冊到 StandardEvaluationContext， 讓 SpEL
	 * 表達式可以直接調用這些方法。
	 * 
	 * @param context
	 */
	public static void methodRegistration(StandardEvaluationContext context) {
		try {
			log.debug("start regist context methods");
			for (Method m : ValidationUtil.class.getDeclaredMethods()) {
				context.registerFunction(m.getName(), m);
			}

			for (Method m : VariableUtil.class.getDeclaredMethods()) {
				context.registerFunction(m.getName(), m);
			}

//            for (Method m : SpelExpressionUtils.class.getDeclaredMethods()) {
//            	context.registerFunction(m.getName(), m);
//            }
//			context.registerFunction("isNotNumericWithComma", ValidationUtils.class.getDeclaredMethod("isNotNumericWithComma", String.class));
//			context.registerFunction("isNumeric", ValidationUtils.class.getDeclaredMethod("isNumeric", String.class));
		} catch (Exception e) {
			log.error("function reg fail");
		}
	}

}
