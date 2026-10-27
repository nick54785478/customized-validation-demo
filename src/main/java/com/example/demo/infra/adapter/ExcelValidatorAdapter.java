package com.example.demo.infra.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.demo.application.exception.ExcelValidationException;
import com.example.demo.application.port.out.ExcelValidatorPort;
import com.example.demo.application.shared.vo.ValidationError;
import com.example.demo.application.shared.vo.YesNo;
import com.example.demo.infra.cv.normalizer.TemplateHeaderNormalizer;
import com.example.demo.infra.cv.normalizer.TemplateSheetNormalizer;
import com.example.demo.infra.cv.shared.context.ContextRoot;
import com.example.demo.infra.cv.validator.CustomValidator;
import com.example.demo.infra.persistence.entity.TemplateFieldMapping;
import com.example.demo.infra.persistence.entity.TemplateSheetMapping;
import com.example.demo.infra.persistence.entity.ValidationPolicy;
import com.example.demo.infra.persistence.repository.TemplateFieldMappingRepository;
import com.example.demo.infra.persistence.repository.TemplateSheetMappingRepository;
import com.example.demo.infra.persistence.repository.ValidationPolicyRepository;
import com.example.demo.infra.util.ExcelUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Excel 範本檢核輸出適配器 (Excel Validator Adapter - Outbound Adapter)
 * <p>
 * <b>架構定位與職責：</b><br>
 * 本類別隸屬於六角形架構（Hexagonal Architecture）之基礎設施層適配器模組（{@code infra.adapter}），
 * 實作應用層定義之輸出埠合約 {@link ExcelValidatorPort}。<br>
 * 作為應用層業務邏輯與外部基礎設施（檔案處理、資料庫策略、工作表與欄位雙層防腐層、客製驗證引擎）之間的協調樞紐：
 * <ul>
 *   <li>接收純 Java 二進位檔案輸入串流 {@link InputStream}，隔絕表現層技術物件對內層的侵入。</li>
 *   <li>調用 {@link ValidationPolicyRepository}、{@link TemplateSheetMappingRepository} 與 {@link TemplateFieldMappingRepository} 讀取策略與雙層對映定義。</li>
 *   <li>利用 {@link ExcelUtil} 將 Excel 資料串流解構為多工作表（Sheet）之行列資料結構。</li>
 *   <li>委派 {@link TemplateSheetNormalizer} 進行第一層工作表前置防呆與頁籤名稱正規化。</li>
 *   <li>委派 {@link TemplateHeaderNormalizer} 進行第二層標題前置防呆校驗與欄位代碼正規化。</li>
 *   <li>委派客製驗證核心 {@link CustomValidator} 評估 SpEL 表達式與資料約束。</li>
 *   <li>將所有不符合規格之錯誤收斂封裝為不可變的值物件清單 {@link ValidationError}，並統一拋出結構化例外 {@link ExcelValidationException}。</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class ExcelValidatorAdapter implements ExcelValidatorPort {

    /**
     * 客製驗證核心引擎 (Custom Validator Engine)
     */
    private final CustomValidator customValidator;

    /**
     * 驗證原則持久化倉儲 (Validation Policy Repository)
     */
    private final ValidationPolicyRepository validationPolicyRepository;

    /**
     * 範本工作表客製對映持久化倉儲 (Template Sheet Mapping Repository)
     */
    private final TemplateSheetMappingRepository templateSheetMappingRepository;

    /**
     * 範本欄位客製對映持久化倉儲 (Template Field Mapping Repository)
     */
    private final TemplateFieldMappingRepository templateFieldMappingRepository;

    /**
     * 工作表名稱正規化防腐器 (Template Sheet Normalizer - Level 1 ACL)
     */
    private final TemplateSheetNormalizer templateSheetNormalizer;

    /**
     * 標題正規化防腐器 (Template Header Normalizer - Level 2 ACL)
     */
    private final TemplateHeaderNormalizer templateHeaderNormalizer;

    /**
     * 建構子注入基礎設施依賴元件
     *
     * @param customValidator                客製驗證核心引擎
     * @param validationPolicyRepository     驗證原則持久化倉儲
     * @param templateSheetMappingRepository 範本工作表對映持久化倉儲
     * @param templateFieldMappingRepository 範本欄位對映持久化倉儲
     * @param templateSheetNormalizer        工作表名稱正規化防腐器
     * @param templateHeaderNormalizer       標題正規化防腐器
     */
    public ExcelValidatorAdapter(
            CustomValidator customValidator,
            ValidationPolicyRepository validationPolicyRepository,
            TemplateSheetMappingRepository templateSheetMappingRepository,
            TemplateFieldMappingRepository templateFieldMappingRepository,
            TemplateSheetNormalizer templateSheetNormalizer,
            TemplateHeaderNormalizer templateHeaderNormalizer) {
        this.customValidator = customValidator;
        this.validationPolicyRepository = validationPolicyRepository;
        this.templateSheetMappingRepository = templateSheetMappingRepository;
        this.templateFieldMappingRepository = templateFieldMappingRepository;
        this.templateSheetNormalizer = templateSheetNormalizer;
        this.templateHeaderNormalizer = templateHeaderNormalizer;
    }

    /**
     * 執行 Excel 範本資料結構與業務規則檢核
     * <p>
     * <b>處理流程：</b>
     * <ol>
     *   <li>依據範本名稱自資料庫取出作用中的驗證策略規則、工作表對映定義與欄位對映定義。</li>
     *   <li>解析二進位輸入串流為以工作表名稱為鍵的資料列矩陣。</li>
     *   <li><b>第一層防腐 (Level 1 ACL)：</b>委派 {@link TemplateSheetNormalizer} 進行工作表防呆與頁籤名稱正規化。</li>
     *   <li><b>第二層防腐 (Level 2 ACL)：</b>委派 {@link TemplateHeaderNormalizer} 進行標題防呆與欄位屬性代碼正規化。</li>
     *   <li>建置客製驗證引擎所需之根上下文環境 {@link ContextRoot}。</li>
     *   <li>呼叫驗證核心評估各項規則，取得檢核失敗之 {@link ValidationError} 清單。</li>
     *   <li>若存在錯誤項目，則立即拋出 {@link ExcelValidationException} 中斷後續業務流程。</li>
     * </ol>
     * </p>
     *
     * @param templateName 範本唯一識別名稱 (例如: "USER_PROFILE")，對應資料庫策略定義
     * @param inputStream  Excel 試算表檔案二進位輸入串流 (Pure Java InputStream)
     * @throws ExcelValidationException 當試算表內容不符業務規則或缺少必要工作表/標題時拋出
     * @throws IllegalStateException    當讀取或解析 Excel 二進位串流發生 I/O 異常時拋出
     */
    @Override
    public void validateExcelData(String templateName, InputStream inputStream) {
        // 步驟 1: 自資料庫取出該範本設定的所有驗證規則、工作表對映與欄位對映設定
        List<ValidationPolicy> policyList = validationPolicyRepository.findByTemplateName(templateName);
        List<TemplateSheetMapping> sheetMappings = templateSheetMappingRepository
                .findByTemplateNameAndActiveFlag(templateName, YesNo.Y);
        List<TemplateFieldMapping> fieldMappings = templateFieldMappingRepository
                .findByTemplateNameAndActiveFlag(templateName, YesNo.Y);

        log.debug("載入範本 [{}] 設定: 策略筆數={}, 工作表對映筆數={}, 欄位對映筆數={}",
                templateName, policyList.size(), sheetMappings.size(), fieldMappings.size());

        try {
            // 步驟 2: 讀取 Excel 試算表串流之工作表資料集
            Map<String, List<Map<String, String>>> rawExcelData;
            if (sheetMappings.isEmpty()) {
                // 若未配置工作表對映，維持向下相容讀取策略工作表清單
                List<String> policySheets = policyList.stream()
                        .map(ValidationPolicy::getTemplateSheetName)
                        .distinct()
                        .toList();
                rawExcelData = ExcelUtil.readExcelData(inputStream, policySheets);
            } else {
                // 若配置工作表對映，讀取工作簿全部工作表交由防腐層識別與正規化
                rawExcelData = ExcelUtil.readAllSheets(inputStream);
            }

            // 步驟 3: 第一層防腐層：工作表層級前置防呆校驗與頁籤代碼正規化（例如「使用者名單」 -> "USER"）
            Map<String, List<Map<String, String>>> sheetNormalizedData = templateSheetNormalizer
                    .normalize(templateName, rawExcelData, sheetMappings);

            // 步驟 4: 第二層防腐層：標題前置防呆校驗與鍵值代碼正規化（例如「姓名」 -> "NAME" / "name"）
            Map<String, List<Map<String, String>>> normalizedExcelData = templateHeaderNormalizer
                    .normalize(templateName, sheetNormalizedData, fieldMappings);

            // 步驟 5: 封裝檢核上下文根物件 (ContextRoot)，提供 SpEL 執行環境所需參數與工作表友善名稱對映字典
            Map<String, String> sheetDisplayNameMap = new LinkedHashMap<>();
            if (sheetMappings != null) {
                for (TemplateSheetMapping mapping : sheetMappings) {
                    sheetDisplayNameMap.put(mapping.getSheetCode(), mapping.getDisplayName());
                }
            }

            ContextRoot contextRoot = ContextRoot.builder()
                    .params(new LinkedHashMap<>())
                    .sheetMap(normalizedExcelData)
                    .sheetDisplayNameMap(sheetDisplayNameMap)
                    .build();

            // 步驟 6: 調用客製驗證引擎執行規則比對與運算
            List<ValidationError> vepList = customValidator.validateExcelData(contextRoot, policyList);

            // 步驟 7: 檢核結果判定，若存在任何違規項則封裝為不可變錯誤清單並拋出應用層例外
            if (!vepList.isEmpty()) {
                log.warn("Excel 範本檢核不通過: templateName={}, 錯誤筆數={}", templateName, vepList.size());
                throw new ExcelValidationException("VALIDATED_FAILED", vepList);
            }

            log.info("Excel 範本檢核通過: templateName={}", templateName);

        } catch (IOException e) {
            log.error("讀取 Excel 串流時發生 I/O 異常: templateName={}", templateName, e);
            throw new IllegalStateException("讀取 Excel 檔案串流失敗，請檢查檔案格式或網路傳輸狀態", e);
        }
    }
}
