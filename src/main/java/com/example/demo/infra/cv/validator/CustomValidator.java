package com.example.demo.infra.cv.validator;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
import org.springframework.stereotype.Component;

import com.example.demo.application.shared.vo.ValidationError;
import com.example.demo.infra.cv.parser.ExcelAddressParser;
import com.example.demo.infra.cv.shared.TemplateLine;
import com.example.demo.infra.cv.shared.context.ContextRoot;
import com.example.demo.infra.persistence.entity.ValidationPolicy;
import com.example.demo.infra.util.ValidationUtil;
import com.example.demo.infra.util.VariableUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 客製驗證核心引擎 (Custom Validator Engine)
 * <p>
 * <b>架構定位與職責：</b><br>
 * 本元件位於基礎設施層客製驗證核心模組（{@code infra.cv.validator}），作為整個檢核模組的心臟。<br>
 * 負責依據資料庫設定的 {@link ValidationPolicy} 檢核原則，透過 Spring Expression Language (SpEL)
 * 針對 Excel 試算表的多工作表（Sheet）與資料列（Row）進行動態規則計算、型別檢查與業務約束判定：
 * <ul>
 *   <li><b>上下文生命週期管理：</b> 初始化 {@link StandardEvaluationContext}，注入試算表資料集、全域參數與環境變數。</li>
 *   <li><b>函式庫動態綁定：</b> 自動反射並註冊 {@link ValidationUtil} 與 {@link VariableUtil} 工具方法至 SpEL 上下文。</li>
 *   <li><b>精準座標對齊：</b> 結合 {@link ExcelAddressParser} 與 {@link TemplateLine} 精確反查實體 Excel 儲存格座標（如 "A1", "C4"）。</li>
 *   <li><b>錯誤事實封裝：</b> 產出符合 DDD 規範的不可變檢核錯誤快照 {@link ValidationError} 清單。</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class CustomValidator {

    /**
     * SpEL 動態表達式解析器實例 (執行緒安全)
     */
    private static final ExpressionParser parser = new SpelExpressionParser();

    /**
     * 執行試算表多工作表資料檢核主流程
     * <p>
     * 遍歷 {@link ContextRoot} 內的所有工作表資料，並依據工作表名稱匹配其專屬的 {@link ValidationPolicy} 策略清單，
     * 依序分發至 ROW 級別或 SHEET 級別之處理器執行檢核。
     * </p>
     *
     * @param contextRoot 試算表根上下文物件，包含所有工作表資料矩陣與執行參數
     * @param policyList  該範本設定之資料庫檢核原則與規則定義清單
     * @return 檢核失敗之 {@link ValidationError} 值物件清單；若無任何違規資料則回傳空清單
     */
    public List<ValidationError> validateExcelData(ContextRoot contextRoot, List<ValidationPolicy> policyList) {

        List<ValidationError> vepList = new ArrayList<>();

        // 取得解析後的所有工作表資料集 Map<SheetName, List<RowMap>>
        Map<String, List<Map<String, String>>> sheetMap = contextRoot.getSheetMap();

        // 依據工作表名稱 (TemplateSheetName) 對檢核原則進行群組化
        Map<String, List<ValidationPolicy>> policyMap = policyList.stream()
                .collect(Collectors.groupingBy(ValidationPolicy::getTemplateSheetName));

        // 逐一走訪每個工作表執行檢核流程
        sheetMap.forEach((sheetName, dataSheet) -> {
            // 提取該工作表所有資料列中出現的欄位映射鍵值集合，並保持插入順序
            Set<String> mappingFieldNames = dataSheet.stream()
                    .flatMap(map -> map.keySet().stream())
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            // 構建欄位映射座標字典 Map<mappingFieldName, TemplateLine>
            Map<String, TemplateLine> templateMap = this.generateTemplateMap(sheetName, mappingFieldNames);

            // 設定當前檢核之工作表資料至上下文
            contextRoot.setSheet(dataSheet);

            // 初始化 SpEL 評估上下文，註冊全域參數與內建靜態驗證函式
            StandardEvaluationContext context = initContext(contextRoot);
            context.setVariable("sheetName", contextRoot.getSheetDisplayName(sheetName));

            // 若該工作表存在對應的驗證策略，則依序執行策略檢核
            if (policyMap.containsKey(sheetName)) {
                List<ValidationPolicy> policies = policyMap.get(sheetName);

                policies.forEach(policy -> {
                    // 分流處理 ROW 級別資料列規則
                    if (StringUtils.equalsIgnoreCase(policy.getType(), "ROW")) {
                        this.processRowPolicy(dataSheet, policy, contextRoot, context, templateMap, vepList);
                    // 分流處理 SHEET 級別整表或全域變數規則
                    } else if (StringUtils.equalsIgnoreCase(policy.getType(), "SHEET")) {
                        this.processSheetPolicy(sheetName, policy, context, contextRoot, templateMap, vepList);
                    }
                });
            }
        });

        return vepList;
    }

    /**
     * 處理單列資料（ROW 級別）之客製規則檢核
     * <p>
     * 遍歷工作表之每一筆資料列，設定當前欄位值與列上下文，並評估 SpEL 布林表達式。<br>
     * 當檢核失敗時，計算出實體 Excel 地址（如 "B3"）並組裝 {@link ValidationError} 加入錯誤清單。
     * </p>
     *
     * @param dataSheet   工作表之全部資料列矩陣
     * @param policy      欲套用之檢核策略與規則定義
     * @param contextRoot 檢核根上下文物件
     * @param context     SpEL 評估上下文
     * @param templateMap 欄位與 Excel 欄位位置對照字典
     * @param vepList     錯誤輸出收集清單
     */
    public void processRowPolicy(List<Map<String, String>> dataSheet, ValidationPolicy policy, ContextRoot contextRoot,
                                 StandardEvaluationContext context, Map<String, TemplateLine> templateMap,
                                 List<ValidationError> vepList) {
        // 遍歷當前工作表之所有資料列 (0-based)
        for (int i = 0; i < dataSheet.size(); i++) {
            contextRoot.setCurrentRow(dataSheet.get(i));
            String mappingFieldName = StringUtils.trimToEmpty(policy.getMappingFieldName());

            // 非強制檢核且欄位為空時略過檢核（避免非必填欄位在無值時觸發檢核報錯）
            if (!StringUtils.equalsIgnoreCase(policy.getRule(), "ENFORCE_ROW_VALIDATION")
                    && isRowFieldBlank(contextRoot, mappingFieldName)) {
                continue;
            }

            // 設置當前評估之儲存格值至上下文
            this.setSingleMappingForRowCellValue(contextRoot, mappingFieldName);

            // 取得預處理後的 SpEL 表達式字串
            String expression = preProcessExpression(policy.getExpression());
            log.debug("[validateExcelData] ROW after preprocessor: {}, {}", mappingFieldName, expression);

            // 評估 SpEL 表達式布林值
            Boolean expressionValue = evaluateExpression(context, expression, contextRoot);

            // 若評估結果不為 true (為 false 或 null)，代表檢核未通過
            if (!Boolean.TRUE.equals(expressionValue)) {
                // 格式化驗證錯誤資訊，因資料列走訪從 0 起算，Excel 行數需帶入 i + 1，並代入工作表顯示名稱
                String displaySheetName = contextRoot.getSheetDisplayName(policy.getTemplateSheetName());
                ValidationError vep = formatRowValidateError(context, templateMap, policy, i + 1, displaySheetName);
                vepList.add(vep);
            }
        }
    }

    /**
     * 處理工作表層級（SHEET 級別）或全域變數注入之檢核邏輯
     * <p>
     * <ul>
     *   <li><b>VARIABLE 規則：</b> 評估表達式將計算值寫入 {@link ContextRoot} 與 SpEL 變數，供後續規則引用。</li>
     *   <li><b>整表檢核規則：</b> 表達式回傳錯誤索引與訊息之對應映射，批次記錄檢核錯誤。</li>
     * </ul>
     * </p>
     *
     * @param sheetName   當前工作表名稱
     * @param policy      檢核原則與規則設定
     * @param context     SpEL 評估上下文
     * @param contextRoot 檢核根上下文物件
     * @param templateMap 欄位與 Excel 欄位位置對照字典
     * @param vepList     錯誤輸出收集清單
     */
    public void processSheetPolicy(String sheetName, ValidationPolicy policy, StandardEvaluationContext context,
                                   ContextRoot contextRoot, Map<String, TemplateLine> templateMap, List<ValidationError> vepList) {
        if (StringUtils.equalsIgnoreCase("VARIABLE", policy.getRule())) {
            // 變數設定型策略：動態計算表達式並注入為上下文變數
            Object obj = parser.parseExpression(policy.getExpression()).getValue(context);
            contextRoot.getParams().put(policy.getMappingFieldName(), obj);
            context.setVariable(policy.getMappingFieldName(), obj);
        } else {
            // 處理 SHEET 級別資料驗證
            String expression = preProcessExpression(policy.getExpression());

            /*
             * 說明： SpEL 的 Expression#getValue(EvaluationContext) 回傳型別為 Object，
             * 編譯期無法得知實際泛型型別，因此轉型為 Map<Integer, String> 時，會產生 unchecked cast 警告。
             * 此處 expression 設計約定回傳型別必為 Map<Integer, String>，且由系統內部控制，風險可控。
             */
            @SuppressWarnings("unchecked")
            Map<Integer, String> expressionValue = (Map<Integer, String>) parser.parseExpression(expression)
                    .getValue(context);

            if (expressionValue != null) {
                TemplateLine templateLine = getTemplateLine(templateMap, policy.getMappingFieldName());
                String displaySheetName = contextRoot.getSheetDisplayName(sheetName);
                expressionValue.forEach((dataRowIndex, errorMessage) -> {
                    // 計算實體 Excel 座標（需加上標題列與 1-based 偏移）
                    String excelAddress = ExcelAddressParser.convertNumToAddress(dataRowIndex + 1,
                            templateLine.getDataColumnNum());
                    log.debug("dataRowIndex:{}, errorMessage:{}", dataRowIndex, errorMessage);
                    vepList.add(new ValidationError("SheetName : " + displaySheetName + ", " + excelAddress + " 資料檢核發生錯誤，" + errorMessage));
                });
            }
            log.debug("expressionValue : {}", expressionValue);
        }
    }

    /**
     * 構建工作表欄位座標對照表 (TemplateMap)
     *
     * @param sheetName         工作表名稱
     * @param mappingFieldNames 該工作表包含之欄位對應鍵集合
     * @return 欄位鍵與 {@link TemplateLine} 座標定義之對照表
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
     * 評估 SpEL 表達式並回傳布林檢核結果
     *
     * @param context     SpEL 評估上下文
     * @param expression  待執行的 SpEL 表達式字串
     * @param contextRoot 檢核根上下文
     * @return 評估之布林結果；若表達式解析異常或非布林則可能為 null
     */
    private Boolean evaluateExpression(StandardEvaluationContext context, String expression, ContextRoot contextRoot) {
        context.setVariable("checkedValue", contextRoot.getCurrentCellValue());
        return parser.parseExpression(expression).getValue(context, Boolean.class);
    }

    /**
     * 使用 SpEL 解析並渲染格式化之錯誤訊息模板
     *
     * @param context      SpEL 評估上下文
     * @param express      包含 SpEL 變數之錯誤訊息表達式模板
     * @param excelAddress Excel 儲存格座標地址 (例如: "B2")
     * @param sheetName    工作表名稱
     * @return 解析完成之具體錯誤訊息字串
     */
    private static String evaluateErrorMsg(StandardEvaluationContext context, String express, String excelAddress,
                                           String sheetName) {
        context.setVariable("excelAddress", excelAddress);
        context.setVariable("sheetName", sheetName);
        return parser.parseExpression(express).getValue(context, String.class);
    }

    /**
     * 判定當前資料列之特定欄位是否為空值
     *
     * @param contextRoot 檢核根上下文物件
     * @param fieldName   欲檢查之欄位名稱
     * @return 若欄位值為 null 或全為空白字元則回傳 {@code true}，否則回傳 {@code false}
     */
    private boolean isRowFieldBlank(ContextRoot contextRoot, String fieldName) {
        String value = contextRoot.getFieldValue(fieldName);
        return StringUtils.isBlank(value);
    }

    /**
     * 設定當前正在處理之資料列特定欄位值至上下文快取
     *
     * @param contextRoot 檢核根上下文物件
     * @param fieldName   欄位名稱
     */
    private void setSingleMappingForRowCellValue(ContextRoot contextRoot, String fieldName) {
        contextRoot.setCurrentCellValue(contextRoot.getFieldValue(fieldName));
    }

    /**
     * 依據欄位映射名稱自對照表中取得其 {@link TemplateLine} 實體位置定義
     *
     * @param templateLineMap 欄位座標對照表
     * @param mappingFieldName 欄位名稱
     * @return 對應之 {@link TemplateLine} 物件
     */
    public static TemplateLine getTemplateLine(Map<String, TemplateLine> templateLineMap, String mappingFieldName) {
        return templateLineMap.get(mappingFieldName);
    }

    /**
     * 預處理表達式字串（保留未來語法擴充與標記置換彈性）
     *
     * @param expression 原始表達式字串
     * @return 預處理後之表達式字串
     */
    private static String preProcessExpression(String expression) {
        return expression;
    }

    /**
     * 格式化並封裝資料列驗證失敗之 {@link ValidationError} 值物件
     *
     * @param context          SpEL 評估上下文
     * @param templateLineMap  欄位座標對照表
     * @param policy           違規之檢核策略與原則
     * @param rowIndex         發生違規之資料列索引 (1-based，包含標題偏移)
     * @param displaySheetName 工作表之友善顯示名稱
     * @return 包含規則代碼、列號與具體錯誤訊息之 {@link ValidationError}
     */
    private static ValidationError formatRowValidateError(StandardEvaluationContext context,
                                                          Map<String, TemplateLine> templateLineMap,
                                                          ValidationPolicy policy,
                                                          int rowIndex,
                                                          String displaySheetName) {
        // 取得 MappingFieldName 對應的 TemplateLine (包含 Excel 欄位索引)
        TemplateLine templateLine = getTemplateLine(templateLineMap,
                StringUtils.trimToEmpty(policy.getMappingFieldName()));
        
        // 換算 Excel 座標，例如 "B3" (此處 +1 補齊標題列偏移)
        String excelAddress = ExcelAddressParser.convertNumToAddress(rowIndex + 1, templateLine.getDataColumnNum());
        log.debug("rowIndex:{}, excelAddress:{}", rowIndex, excelAddress);

        // 使用 SpEL 解析渲染錯誤訊息模板，傳入友善的工作表顯示名稱
        String errorMessage = evaluateErrorMsg(context, policy.getErrorMessage(), excelAddress,
                displaySheetName);

        return new ValidationError(policy.getRule(), rowIndex, errorMessage);
    }

    /**
     * 格式化並封裝資料列驗證失敗之 {@link ValidationError} 值物件（向下相容舊簽章）
     */
    private static ValidationError formatRowValidateError(StandardEvaluationContext context,
                                                          Map<String, TemplateLine> templateLineMap,
                                                          ValidationPolicy policy,
                                                          int rowIndex) {
        return formatRowValidateError(context, templateLineMap, policy, rowIndex, policy.getTemplateSheetName());
    }

    /**
     * 初始化 SpEL 上下文環境，注入工作表資料、全域參數與內建靜態驗證函式
     *
     * @param contextRoot 檢核根上下文物件
     * @return 初始化完成之 {@link StandardEvaluationContext}
     */
    public StandardEvaluationContext initContext(ContextRoot contextRoot) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        // 載入當前工作表之資料矩陣
        Map<String, Object> params = contextRoot.getParams();
        params.put("sheet", contextRoot.getSheet());
        contextRoot.setParams(params);

        // 綁定動態自訂參數為 SpEL 變數
        this.setContextVariable(contextRoot, context);

        // 反射註冊內建工具類靜態方法至 SpEL
        methodRegistration(context);

        return context;
    }

    /**
     * 將根上下文內的參數字典批量設定至 SpEL 變數集合
     * <p>
     * 設定後在 SpEL 表達式中可直接以 {@code #參數名} 取用，例如：<br>
     * {@code #minValue < #checkedValue && #checkedValue < #maxValue}
     * </p>
     *
     * @param contextRoot 檢核根上下文物件
     * @param context     SpEL 評估上下文
     */
    private void setContextVariable(ContextRoot contextRoot, StandardEvaluationContext context) {
        for (Map.Entry<String, Object> p : contextRoot.getParams().entrySet()) {
            context.setVariable(p.getKey(), p.getValue());
        }
    }

    /**
     * 反射註冊 {@link ValidationUtil} 與 {@link VariableUtil} 的所有公共靜態方法至 SpEL
     * <p>
     * 讓 SpEL 規則能直接以函式名稱調用常用驗證與轉換邏輯（例如 {@code isNumeric(...)} 等）。
     * </p>
     *
     * @param context SpEL 評估上下文
     */
    public static void methodRegistration(StandardEvaluationContext context) {
        try {
            log.debug("開始註冊 SpEL 上下文內建工具方法");
            for (Method m : ValidationUtil.class.getDeclaredMethods()) {
                log.debug("註冊 ValidationUtil 方法: {}", m.getName());
                context.registerFunction(m.getName(), m);
            }

            for (Method m : VariableUtil.class.getDeclaredMethods()) {
                log.debug("註冊 VariableUtil 方法: {}", m.getName());
                context.registerFunction(m.getName(), m);
            }
        } catch (Exception e) {
            log.error("SpEL 函式註冊過程發生異常", e);
        }
    }
}
