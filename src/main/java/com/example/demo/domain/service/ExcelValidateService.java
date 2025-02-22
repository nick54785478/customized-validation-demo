package com.example.demo.domain.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
import com.example.demo.util.ExcelParseUtil;
import com.example.demo.util.ValidationUtil;
import com.example.demo.util.VariableUtil;
import com.google.common.primitives.Ints;

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
		Map<String, List<Map<String, String>>> sheetMap = contextRoot.getSheetMap();

		// 根據 SheetName 進行 policy 分群
		Map<String, List<ValidationPolicy>> policyMap = policyList.stream()
				.collect(Collectors.groupingBy(ValidationPolicy::getTemplateSheetName));

		// 遍歷 SheetMap
		sheetMap.forEach((sheetName, dataSheet) -> {
			// 設置當前的 Sheet
			contextRoot.setSheet(dataSheet);
			// 初始化 SpEL 上下文，準備變數與方法。
			StandardEvaluationContext context = initContext(contextRoot);
			if (policyMap.containsKey(sheetName)) {
				List<ValidationPolicy> policies = policyMap.get(sheetName);

				// 生成 TemplateMap Map<mappingFieldName, TemplateLine>
				Map<String, TemplateLine> templateMap = this.generateTemplateMap(policies);

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
							// 處理 SHEET 的 Policy，取得預處理表達式，可擴展替換特殊標記
							String expression = preProcessExpression(policy.getExpression());
							// 執行 Expression，並回傳 Map< rowIndex, errorMessage >
							Map<Integer, String> expressionValue = (Map<Integer, String>) parser
									.parseExpression(expression).getValue(context);

							if (expressionValue != null) {
								TemplateLine templateLine = getTemplateLine(templateMap, policy.getMappingFieldName());
								expressionValue.forEach((dataRowIndex, errorMessage) -> {
									// 取得 ExcelAddress，這邊需 +1，因為是 1-based
									String excelAddress = ExcelParseUtil.convertNumToAddress(dataRowIndex + 1,
											templateLine.getDataColumnNum());
									ValidateErrorProperty vep = new ValidateErrorProperty();
									vep.setMessage(excelAddress + " 資料檢核發生錯誤，" + errorMessage);
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
	 * 對整個 SHEET 的資料驗證
	 */
	public List<ValidateErrorProperty> validateExcelSheetData(ContextRoot contextRoot,
			List<ValidationPolicy> policyList, List<ValidateErrorProperty> vepList) {

		// 初始化 SpEL 上下文，準備變數與方法。
		StandardEvaluationContext context = initContext(contextRoot);

		// 根據 SheetName 及 驗證條件:SHEET 過濾出該 Policy 清單
		List<ValidationPolicy> filteredList = policyList.stream().filter(e -> StringUtils.equals(e.getType(), "SHEET"))
				.collect(Collectors.toList());

		// 紀錄 Template Line
		List<Map<String, String>> dataSheet = contextRoot.getSheet();
		Map<String, TemplateLine> templateMap = generateTemplateMap(policyList);

		// 過濾出針對 SHEET 做驗證的 policy
		Map<String, ValidationPolicy> validateMap = policyList.stream()
				.collect(Collectors.toMap(ValidationPolicy::getMappingFieldName, Function.identity()));

		filteredList.stream().forEach(policy -> {
			// 如果是 VARIABLE => 進行變數設置
			if (StringUtils.equalsIgnoreCase("VARIABLE", policy.getRule())) {
				Object obj = parser.parseExpression(policy.getExpression()).getValue(context);
				contextRoot.getParams().put(policy.getMappingFieldName(), obj);
				context.setVariable(policy.getMappingFieldName(), obj);
			} else {
//				// 不是的話執行驗證
//				Map<String, String> expressionValue = (Map<String, String>) parser
//						.parseExpression(policy.getExpression()).getValue(context);
//				if (expressionValue != null) {
//					// 取得該 MappingFieldName 對應的 TemplateLine
//					TemplateLine templateLine = templateMap.get(policy.getMappingFieldName());
//					
//				}

			}
		});

		return new ArrayList<>();
	}

//	/**
//	 * 轉換 rowIndex 為 Excel 位置，例如 C4、D2
//	 *
//	 * @param errorMap         Map<Integer, String>，存放 rowIndex 及錯誤訊息
//	 * @param templateMap      Map<mappingFieldName, TemplateLine>，用來獲取欄位的 Excel
//	 *                         Column Index
//	 * @param mappingFieldName 欲檢查的欄位名稱
//	 * @param dataStartRow     Excel 資料起始列（如資料從第 2 行開始，則 dataStartRow = 2）
//	 * @return Map<String, String> (Excel 位置, errorMessage)
//	 */
//	public static Map<String, String> convertRowIndexToExcelPosition(Map<Integer, String> errorMap,
//			Map<String, TemplateLine> templateMap, String mappingFieldName, int dataStartRow) {
//		Map<String, String> expressionValue = new LinkedHashMap<>();
//		// 獲取 Excel 欄位索引
//		TemplateLine templateLine = templateMap.get(mappingFieldName);
//		if (templateLine == null) {
//			throw new IllegalArgumentException("無法找到對應的 TemplateLine: " + mappingFieldName);
//		}
//		int columnIndex = templateLine.getDataColumnNum(); // Excel 欄索引
//
//		for (Map.Entry<Integer, String> entry : errorMap.entrySet()) {
//			int rowIndex = entry.getKey();
//			String excelPosition = ExcelParseUtil.convertNumToAddress(rowIndex + dataStartRow, columnIndex);
//			expressionValue.put(excelPosition, entry.getValue());
//		}
//
//		return expressionValue;
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
			String mappingFieldName = StringUtils.trimToEmpty(policy.getMappingFieldName());
			// 避免後蓋前
			if (!templateMap.containsKey(mappingFieldName)) {
				templateMap.put(mappingFieldName,
						new TemplateLine(policy.getTemplateSheetName(), mappingFieldName, columnIndex));
			}
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
		// 可加強處理，例如替換特殊標記
		return expression;
	}

	/**
	 * 格式化驗證錯誤資訊。
	 * 
	 * @param context
	 * @param templateLineMap
	 * @param policy
	 * @param rowIndex
	 */
	private static ValidateErrorProperty formatRowValidateError(StandardEvaluationContext context,
			Map<String, TemplateLine> templateLineMap, ValidationPolicy policy, int rowIndex) {
		// 取得 MappingFieldName 對應的 TemplateLine (包含 Excel Column Index)
		TemplateLine templateLine = getTemplateLine(templateLineMap,
				StringUtils.trimToEmpty(policy.getMappingFieldName()));
		// 取得 Excel 地址，如 "B1"
		String excelAddress = ExcelParseUtil.convertNumToAddress(rowIndex + 1, templateLine.getDataColumnNum());
		log.debug("rowIndex:{}, excelAddress:{}", rowIndex, excelAddress);
		// 使用 SpEL 解析錯誤訊息
		String errorMessage = evaluateErrorMsg(context, policy.getErrorMessage(), excelAddress,
				policy.getTemplateSheetName());
		return new ValidateErrorProperty(policy.getRule(), rowIndex, errorMessage);
	}

	/**
	 * 初始化 SpEL 上下文，準備變數與方法。
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
	 * 將 ValidationUtils 和 VariableUtil 內的所有方法註冊到 SpEL，讓 SpEL 表達式可以直接調用這些方法。
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
