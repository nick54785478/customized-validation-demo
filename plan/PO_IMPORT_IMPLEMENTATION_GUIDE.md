# PO Import (採購訂單批次匯入) 客製化驗證實作指南

> **文件版本**：v1.0.0 (正式發布版)  
> **建立日期**：2026-10-29  
> **適用範本**：`PO_IMPORT` (Purchase Order Batch Import)  
> **依循架構規範**：[AGENTS.md](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/.agents/AGENTS.md) (DDD / Clean Architecture / 雙層防腐層 / 宣告式 @CustomValidate / SpEL 驗證核心)

---

## 一、 業務情境與核心設計目標 (Overview & Goals)

### 1.1 業務背景 (Business Scenario)
在企業供應鏈與採購作業中，**PO（Purchase Order，採購訂單）** 是企業向合格供應商發出採購確認的正式法律單據。各採購部門、專案單位或供應商系統經常透過 Excel 試算表（批次排程或 MinIO/S3 檔案串流）匯入採購單明細。

系統必須確保上傳的採購單資料在正式入庫並通知外部供應商之前，其格式、工作表別名、欄位標題與各項商業規則（單號編碼、項次、供應商資格、計量單位、單價、幣別、交期與料號重複性）皆已通過嚴密的格式與防腐檢核。

### 1.2 核心痛點與防腐設計目標
1. **業務語言與系統代碼解耦 (Decoupling Business & System Names)**：
   - 外部 Excel 使用直觀的中文工作表名稱（如「採購訂單明細」）與中文標題（如「採購單號」、「供應商代碼」、「採購單價」）。
   - 後端領域模型與 DTO 統一使用標準代碼（如 `PO_ITEM`、`poNo`、`vendorCode`、`unitPrice`）。
2. **兩階段快速失敗 (Two-Phase Fail-Fast)**：
   - **第一層 (Level 1)**：在解析資料列前，先檢驗必備工作表「採購訂單明細」是否存在，缺少即刻中斷。
   - **第二層 (Level 2)**：在計算商業規則前，先檢驗各工作表必備欄位標題是否存在（如缺少「採購單價」或「約定交期」），缺少即刻中斷於第 0 列。
3. **多來源與雲原生串流支援 (MinIO / S3 Stream Native Support)**：
   - 透過 [BatchFileProcessor.java](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/iface/schedule/processor/BatchFileProcessor.java) 的 `processStream(InputStream)`，原生支援 MinIO / S3 物件儲存二進位串流，免除本機磁碟暫存寫入，提升容器化處理效能。
4. **宣告式切面防護 (@CustomValidate)**：
   - Inbound Adapter（如 `UploadController` 與 `PoImportFileProcessor`）只需標註 `@CustomValidate(templateName = "PO_IMPORT")`，即可自動觸發前置檢核，防止不合規資料污染核心應用層。

---

## 二、 雙層防腐架構與資料流 (Two-Tier Architecture Data Flow)

```mermaid
flowchart TD
    subgraph Client_Layer [表現層 / Client 來源]
        A1["Web 上傳 (UploadController)<br>或 MinIO/S3 串流 (PoImportFileProcessor)<br>工作表: 【採購訂單明細】"]
    end

    subgraph Aspect_Layer [宣告式驗證切面 / AOP]
        A2{"@CustomValidate 攔截<br>動態或靜態解析範本: PO_IMPORT"}
    end

    subgraph Infrastructure_Adapter [基礎設施適配器 / ExcelValidatorAdapter]
        B["ExcelUtil.readAllSheets(InputStream)<br>解構為原始多工作表 Map 矩陣"]
        
        subgraph Level_1_ACL [Level 1 ACL: 工作表防腐層 (TemplateSheetNormalizer)]
            C1{"核對必備工作表<br>(PO_ITEM / 採購訂單明細)"}
            C2["頁籤名稱正規化:<br>「採購訂單明細」 ➔ PO_ITEM"]
        end
        
        subgraph Level_2_ACL [Level 2 ACL: 欄位標題防腐層 (TemplateHeaderNormalizer)]
            D1{"核對必備欄位標題<br>(採購單號, 供應商代碼, 單價...)"}
            D2["標題代碼正規化:<br>中文標題 ➔ CamelCase 屬性代碼<br>(採購單號 ➔ poNo, 採購單價 ➔ unitPrice...)"]
        end
    end

    subgraph Infrastructure_CV_Engine [客製驗證核心 / CustomValidator]
        E["ContextRoot 上下文組裝<br>綁定 normalizedExcelData 與 sheetDisplayNameMap"]
        F1["SpEL 變數注入: #validVendors, #validCurrencies, #validUnits"]
        F2["SpEL 整表檢核: #validateDuplicate(#sheet, 'itemNo')"]
        F3["SpEL 逐列檢核: 單號正則、項次正整數、合格廠商、有效幣別、標準單位、單價、交期"]
    end

    subgraph Exit_Status [出口狀態]
        G1["【校驗失敗】<br>統一拋出 ExcelValidationException<br>由 GlobalExceptionHandler 轉為 HTTP 422 JSON"]
        G2["【校驗成功】<br>產出乾淨之標準結構化資料<br>流轉至應用層 UploadCommandService 入庫"]
    end

    A1 --> A2
    A2 --> B
    B --> C1
    C1 -- "缺少工作表" --> G1
    C1 -- "通過" --> C2
    C2 --> D1
    D1 -- "缺少必備標題" --> G1
    D1 -- "通過" --> D2
    D2 --> E
    E --> F1 --> F2 --> F3
    F3 -- "存在資料違規" --> G1
    F3 -- "全數合規" --> G2
```

---

## 三、 第一部分：範本對映規格說明 (Template Mapping Specification)

範本唯一識別名稱代碼為：`PO_IMPORT`。

### 3.1 Level 1：工作表層級對映設定 (`TemplateSheetMapping`)

負責外部 Excel 實體工作表（Sheet）名稱與系統內部標準工作表代碼的防腐對齊。

| 屬性欄位 | 設定值 | 說明 |
| :--- | :--- | :--- |
| **範本唯一名稱 (`templateName`)** | `PO_IMPORT` | 範本唯一識別碼 |
| **內部工作表代碼 (`sheetCode`)** | `PO_ITEM` | 系統內部業務統一代碼 |
| **外部顯示名稱 (`displayName`)** | `採購訂單明細` | 外部 Excel 實體頁籤名稱（支援雙向別名相容，填寫 `PO_ITEM` 或 `採購訂單明細` 均可辨識） |
| **預期工作表索引 (`sheetIndex`)** | `0` | 首頁工作表（0-based，非強制） |
| **是否必備 (`isRequired`)** | `Y` | 前置防呆：缺少此工作表立即中斷並拋錯 |
| **啟用註記 (`activeFlag`)** | `Y` | 是否啟用此對映 |

---

### 3.2 Level 2：欄位標題層級對映設定 (`TemplateFieldMapping`)

負責外部 Excel 首列實體中文標題與系統內部屬性鍵值（CamelCase）的對齊與順序規範。

| 實體中文標題 (`displayName`) | 內部屬性代碼 (`fieldCode`) | 預期順序 (`columnIndex`) | 是否必填標題 (`isRequired`) | 資料型別 (`dataType`) | 說明 |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **採購單號** | `poNo` | `1` | `Y` | `STRING` | 採購單編號 (`PO-YYYY-NNNN`)，同單明細單號一致 |
| **項次** | `lineNo` | `2` | `Y` | `NUMBER` | 採購單明細項次 (正整數，如 10, 20) |
| **供應商代碼** | `vendorCode` | `3` | `Y` | `STRING` | 供應商系統代碼 (核准合格廠商) |
| **物料料號** | `itemNo` | `4` | `Y` | `STRING` | 採購料號，同單不重複 |
| **物料品名** | `itemName` | `5` | `Y` | `STRING` | 物料中文品名 |
| **採購數量** | `quantity` | `6` | `Y` | `NUMBER` | 採購數量（必須為正整數） |
| **計量單位** | `unit` | `7` | `Y` | `STRING` | 標準計量單位 (如 PC, KG, SET) |
| **採購單價** | `unitPrice` | `8` | `Y` | `NUMBER` | 採購單價（必須為正數） |
| **幣別** | `currency` | `9` | `Y` | `STRING` | 結算幣別 (如 TWD, USD, EUR) |
| **約定交期** | `deliveryDate` | `10` | `Y` | `DATE` | 廠商承諾交期 (`YYYY-MM-DD`) |
| **備註** | `remark` | `11` | `N` | `STRING` | 採購補充說明 (非必備欄位) |

> **防腐效益**：
> 1. 上傳若缺少標記 `isRequired = Y` 的任何欄位標題（例如誤刪「採購單價」或「約定交期」），第二層防腐層將在**第 0 列**直接攔截，防止後續資料列解析產生非預期的空值例外。
> 2. 後續的所有檢核規則（Validation Policy）與應用層 DTO 轉換，均能基於統一的 CamelCase 屬性代碼（如 `poNo`, `vendorCode`, `unitPrice`）運作，免除繁瑣的手動轉對映邏輯。

---

## 四、 第二部分：客製檢核規則設定 (Validation Policy Specification)

透過 SpEL 動態表達式，設定對應的資料約束。所有規則儲存於策略設定清單中。

### 4.1 檢核原則清單全貌

| 優先序 | 類型 (`type`) | 檢核欄位 (`mappingFieldName`) | 規則代碼 (`rule`) | SpEL 表達式 (`expression`) | 錯誤訊息模板 (`errorMessage`) | 業務約束說明 |
| :---: | :---: | :--- | :--- | :--- | :--- | :--- |
| **1** | `SHEET` | `validVendors` | `VARIABLE` | `{''V1001'', ''V1002'', ''V1003'', ''V2001'', ''V8888''}` | *(無)* | **環境變數注入**：宣告核准合格供應商名單 |
| **2** | `SHEET` | `validCurrencies` | `VARIABLE` | `{''TWD'', ''USD'', ''EUR'', ''JPY'', ''CNY''}` | *(無)* | **環境變數注入**：宣告核准交易幣別清單 |
| **3** | `SHEET` | `validUnits` | `VARIABLE` | `{''PC'', ''KG'', ''SET'', ''BOX'', ''M'', ''EA''}` | *(無)* | **環境變數注入**：宣告標準計量單位清單 |
| **4** | `SHEET` | `itemNo` | `VALIDATE_DUPLICATE` | `#validateDuplicate(#sheet, ''itemNo'')` | *(由函式動態產出)* | **整表唯一性檢核**：同張訂單內物料料號不可重複 |
| **5** | `ROW` | `poNo` | `ENFORCE_ROW_VALIDATION` | `#checkedValue != null && #checkedValue.matches(''^PO-\\d{4}-\\d{4}$'')` | `'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [採購單號] 格式不合規，需為 PO-YYYY-NNNN (例如 PO-2026-0001)'` | **編碼規則**：採購單號格式正則檢核 |
| **6** | `ROW` | `lineNo` | `ENFORCE_ROW_VALIDATION` | `#isNumeric(#checkedValue) && T(java.lang.Integer).parseInt(#checkedValue) > 0` | `'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [項次] 必須為大於 0 的正整數，當前值為 [' + #checkedValue + ']'` | **型別與邊界**：項次必須為大於 0 的正整數 |
| **7** | `ROW` | `vendorCode` | `ENFORCE_ROW_VALIDATION` | `#validVendors.contains(#checkedValue)` | `'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [供應商代碼] [' + #checkedValue + '] 不存在或非核准合格供應商'` | **廠商資格**：比對規則 1 所注入的供應商變數清單 |
| **8** | `ROW` | `itemNo` | `ENFORCE_ROW_VALIDATION` | `#checkedValue != null && #checkedValue.trim() != ''''` | `'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [物料料號] 為必填項目，不可為空'` | **必填檢查**：物料料號不得為空 |
| **9** | `ROW` | `itemName` | `ENFORCE_ROW_VALIDATION` | `#checkedValue != null && #checkedValue.trim() != ''''` | `'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [物料品名] 為必填項目，不可為空'` | **必填檢查**：物料品名不得為空 |
| **10** | `ROW` | `quantity` | `ENFORCE_ROW_VALIDATION` | `#isNumeric(#checkedValue) && T(java.lang.Integer).parseInt(#checkedValue) > 0` | `'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [採購數量] 必須為大於 0 的正整數，當前值為 [' + #checkedValue + ']'` | **型別與邊界**：數量必須為正整數且 > 0 |
| **11** | `ROW` | `unit` | `ENFORCE_ROW_VALIDATION` | `#validUnits.contains(#checkedValue)` | `'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [計量單位] [' + #checkedValue + '] 不合規，需為標準計量單位 (如 PC, KG, SET)'` | **單位合法性**：比對規則 3 所注入的計量單位清單 |
| **12** | `ROW` | `unitPrice` | `ENFORCE_ROW_VALIDATION` | `#isNumeric(#checkedValue) && T(java.lang.Double).parseDouble(#checkedValue) > 0` | `'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [採購單價] 必須為大於 0 的有效數值，當前值為 [' + #checkedValue + ']'` | **型別與邊界**：單價必須為數值且 > 0 |
| **13** | `ROW` | `currency` | `ENFORCE_ROW_VALIDATION` | `#validCurrencies.contains(#checkedValue)` | `'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [幣別] [' + #checkedValue + '] 不合規，需為核准幣別 (如 TWD, USD, EUR)'` | **幣別合法性**：比對規則 2 所注入的核准幣別清單 |
| **14** | `ROW` | `deliveryDate` | `ENFORCE_ROW_VALIDATION` | `#checkedValue != null && #checkedValue.matches(''^\\d{4}-\\d{2}-\\d{2}$'')` | `'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [約定交期] 格式不合規，需為 YYYY-MM-DD (例如 2026-12-31)'` | **格式檢核**：日期必須嚴格符合 ISO 格式 |

---

### 4.2 SpEL 核心語法與變數對照表

檢核引擎（[CustomValidator](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/infra/cv/validator/CustomValidator.java)）在初始化評估上下文（`StandardEvaluationContext`）時，已預先注入以下上下文變數，並透過 Java 反射機制自動將 [ValidationUtil.java](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/infra/util/ValidationUtil.java) 與 [VariableUtil.java](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/infra/util/VariableUtil.java) 的所有靜態方法註冊為 SpEL 全域函式。

#### 1. 上下文環境變數 (Context Variables)

| 變數名稱 | 型態 | 說明與取用範例 |
| :--- | :--- | :--- |
| **`#checkedValue`** | `String` | 當前評估之儲存格字串值（如 `"PO-2026-0001"`、`"V1001"`、`"45000"`）。在 ROW 級別規則中由引擎自動更新為當前儲存格內容。 |
| **`#sheet`** | `List<Map<String, String>>` | 當前工作表之所有已正規化資料列矩陣。 |
| **`#sheetName`** | `String` | 當前工作表外部友善顯示名稱（如 `"採購訂單明細"`），供錯誤訊息模板使用。 |
| **`#excelAddress`** | `String` | 實體 Excel 座標字串（如 `"A2"`、`"D3"`），由資料行號與欄位順序自動動態運算生成。 |
| **自訂全域變數** | `Object` / `Collection` | 透過 `rule = 'VARIABLE'` 策略設定的變數名稱，如 `#validVendors`、`#validCurrencies`、`#validUnits`。 |

#### 2. ValidationUtil 內建資料校驗函式庫 (自動反射註冊)

由 [ValidationUtil.java](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/infra/util/ValidationUtil.java) 提供，專注於欄位值格式、邊界、包含性與重複性校驗：

| SpEL 調用函式 | 傳參規格 | 說明與典型使用情境 |
| :--- | :--- | :--- |
| **`#validateDuplicate(#sheet, 'fieldName')`** | `(List<Map>, String)` | **整表唯一性檢核**：遍歷工作表，當發現指定欄位有重複值時，記錄首次出現與後續所有重複行號，回傳 `Map<行號, 錯誤訊息>`。 |
| **`#contains(#checkList, #sheet, 'fieldName')`** | `(List<String>, List<Map>, String)` | **清單存在性檢核**：內部轉為 `HashSet` 以達 $O(1)$ 效率，檢核該欄位值是否皆存在於限定清單內。 |
| **`#isNumeric(#checkedValue)`** | `(String)` | **數值格式檢核**：使用正則表達式 `^-?[\d]+(\.[\d]+)?$` 安全判斷是否為數值（支援負數與浮點數），避免拋出 `NumberFormatException`。 |
| **`#isNotNumeric(#checkedValue)`** | `(String)` | **非數值判斷**：等同於 `!#isNumeric(#checkedValue)`。 |
| **`#isNotNumericWithComma(#checkedValue)`** | `(String)` | **排除千分位之數值判斷**：自動移除逗號（`,`）後判斷是否為數值。 |
| **`#isInteger(#checkedValue)`** | `(String)` | **整數型別轉換檢核**：嘗試轉換為 32-bit Integer，測試數值是否為有效整數。 |

#### 3. VariableUtil 動態集合運算與投影工具庫 (自動反射註冊)

由 [VariableUtil.java](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/infra/util/VariableUtil.java) 提供，專注於將工作表在記憶體中投影轉換為各類資料結構，支援跨表關聯與複雜條件集合運算：

| SpEL 調用函式 | 傳參規格 | 說明與典型使用情境 |
| :--- | :--- | :--- |
| **`#toSet(#sheet, 'fieldName')`** | `(List<Map>, String)` | **欄位值去重集合**：將指定欄位資料投影為 `LinkedHashSet<String>`，常作為跨工作表外鍵參照的母體清單。 |
| **`#toAdjSet(#sheet, 'fieldName', 'adjFieldName')`** | `(List<Map>, String, String)` | **條件過濾投影集合**：僅過濾出判定旗標（如 `activeFlag == "Y"`）的資料列，再將目標欄位投影為 `Set`。 |
| **`#toList(#sheet, 'fieldName')`** | `(List<Map>, String)` | **欄位清單投影**：將工作表指定欄位投影為保有輸入順序與重複值之 `ArrayList<String>`。 |
| **`#diffSet(#set1, #set2)`** | `(Set, Set)` | **集合差集運算**：計算 `set1 - set2`，快速抓出未存在於基準集合中的違規值。 |
| **`#intersectionSet(#set1, #set2)`** | `(Set, Set)` | **集合交集運算**：計算 `set1 ∩ set2` 之共同元素集合。 |
| **`#toMap(#sheet, 'keyField', 'valueField')`** | `(List<Map>, String, String)` | **雙欄位鍵值對照表**：將工作表兩欄位動態組裝為 `Map<Object, Object>`，利於 SpEL 進行鍵查值檢核。 |

---

## 五、 第三部分：程式碼串接與執行流程 (Code Integration Walkthrough)

### 5.1 呼叫輸出適配器 (`ExcelValidatorPort`)

在應用層 UseCase 或 Service 中，調用六角形架構的 Outbound Port 介面執行驗證：

```java
@Service
public class UploadPoApplicationService {

    private final ExcelValidatorPort excelValidatorPort;

    public UploadPoApplicationService(ExcelValidatorPort excelValidatorPort) {
        this.excelValidatorPort = excelValidatorPort;
    }

    public void execute(InputStream inputStream) {
        // 一行指令完成雙層防腐正規化與全套業務規則檢核
        // 若有任何缺漏或資料違規，將自動拋出 ExcelValidationException
        excelValidatorPort.validateExcelData("PO_IMPORT", inputStream);
        
        // 通過檢核後，執行後續 PO 採購訂單領域物件建立、供應商通知與持久化...
    }
}
```

### 5.2 內部適配器執行步驟分析 (`ExcelValidatorAdapter`)
適配器內部按照兩階段快速失敗與防腐機制協調整合：
1. **載入策略定義**：自倉儲取得 `PO_IMPORT` 的 `TemplateSheetMapping`、`TemplateFieldMapping` 與 `ValidationPolicy`。
2. **解構工作簿**：透過 `ExcelUtil.readAllSheets(inputStream)` 取得所有頁籤原始資料。
3. **Level 1 防腐**：調用 `templateSheetNormalizer.normalize(...)`。若缺少「採購訂單明細」頁籤立即拋出 `SHEET_VALIDATION_FAILED`。
4. **Level 2 防腐**：調用 `templateHeaderNormalizer.normalize(...)`。若缺少「採購單號」、「採購單價」等必填標題立即拋出 `HEADER_VALIDATION_FAILED`。
5. **引擎運算**：組裝 `ContextRoot`，調用 `customValidator.validateExcelData(...)` 進行 SpEL 規則運算。
6. **錯誤收斂**：若產生任何 `ValidationError`，封裝並拋出結構化 `ExcelValidationException`。

---

### 5.3 表現層上傳 REST API 呼叫規格 (Upload API Specification & Request Data)

系統提供標準 Multipart 檔案上傳 API 端點，支援前端或供應商系統將 PO Excel 檔案與中繼資訊一同上傳並觸發校驗：

#### 1. API 基本端點資訊
* **HTTP Method**：`POST`
* **Request URL**：`/api/v1/upload` (例如 `http://localhost:8080/api/v1/upload`)
* **Content-Type**：`multipart/form-data`
* **宣告式驗證註解**：[`@CustomValidate(templateName = "#resource.name")`](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/application/annotation/CustomValidate.java)
* **Controller**：[UploadController](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/iface/rest/UploadController.java)
* **驗證攔截切面**：[CustomValidationAspect](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/iface/aspect/CustomValidationAspect.java)

#### 2. Request Data 參數結構 (Multipart Form-Data)

| 參數鍵名 (`Key`) | 參數型態 | 內容型別 (`Content-Type`) | 是否必填 | 說明與範例值 |
| :--- | :--- | :--- | :---: | :--- |
| **`resource`** | JSON 文字區塊 | `application/json` | **是** | 範本中繼資訊，映射至 [UploadTemplateResource](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/iface/resource/in/UploadTemplateResource.java) |
| **`file`** | 二進位檔案 | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` | **是** | 待檢核之 Excel 檔案（如 `PO_IMPORT.xlsx`） |

##### `resource` 參數 JSON 結構明細：
```json
{
  "name": "PO_IMPORT",
  "type": "PO",
  "fileType": "xlsx",
  "filePath": "/TEMPLATE/PO_IMPORT",
  "fileName": "PO_IMPORT.xlsx"
}
```

* **欄位校驗規則**：
  * `name`：範本唯一識別碼，**必填**，對應 `validation_policy` 與 `template_field_mapping` 的 `template_name`。
  * `type`：範本種類代碼，**必填**（如 `"PO"`、`"EXCEL"`）。
  * `fileType`：副檔名類型（如 `"xlsx"`）。
  * `filePath`：伺服器暫存儲存路徑（選填）。
  * `fileName`：原始檔案名稱（選填）。

---

#### 3. 呼叫範例 (Invocation Examples)

##### (1) cURL 命令範例
```bash
curl -X POST "http://localhost:8080/api/v1/upload" \
  -H "Content-Type: multipart/form-data" \
  -F 'resource={
    "name": "PO_IMPORT",
    "type": "PO",
    "fileType": "xlsx",
    "filePath": "/TEMPLATE/PO_IMPORT",
    "fileName": "PO_IMPORT.xlsx"
  };type=application/json' \
  -F "file=@d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/resources/templates/PO_IMPORT.xlsx"
```

##### (2) Postman 設定步驟
參照專案根目錄之 [customized_validation-demo.postman_collection.json](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/customized_validation-demo.postman_collection.json)：
1. 設定 Request Method 為 **`POST`**，URL 為 `http://localhost:8080/api/v1/upload`。
2. 切換至 **Body** 標籤，選擇 **`form-data`** 模式。
3. 新增欄位 1：
   * Key：`resource`
   * Type：`Text`
   * Content-Type：設為 `application/json`
   * Value：
     ```json
     {
       "name": "PO_IMPORT",
       "type": "PO",
       "fileType": "xlsx",
       "filePath": "/TEMPLATE/PO_IMPORT",
       "fileName": "PO_IMPORT.xlsx"
     }
     ```
4. 新增欄位 2：
   * Key：`file`
   * Type：選擇 `File`
   * Value：選擇本機的 `PO_IMPORT.xlsx` 檔案。

##### (3) 前端 JavaScript / Fetch API 範例
```javascript
const uploadPoExcel = async (fileBlob) => {
  const formData = new FormData();

  // 1. 組裝 resource JSON Part
  const resourceData = {
    name: "PO_IMPORT",
    type: "PO",
    fileType: "xlsx",
    filePath: "/TEMPLATE/PO_IMPORT",
    fileName: "PO_IMPORT.xlsx"
  };
  formData.append("resource", new Blob([JSON.stringify(resourceData)], { 
    type: "application/json" 
  }));

  // 2. 組裝 Excel File Part
  formData.append("file", fileBlob, "PO_IMPORT.xlsx");

  // 3. 發送非同步 POST 請求
  const response = await fetch("http://localhost:8080/api/v1/upload", {
    method: "POST",
    body: formData
  });

  const result = await response.json();
  if (response.ok) {
    console.log("上傳並校驗成功:", result);
  } else {
    console.error("校驗失敗，錯誤項目:", result.messages);
  }
};
```

##### (4) Swagger UI 線上互動測試 (Interactive OpenAPI Testing)
專案已整合 OpenAPI 3.0 與 Swagger UI：
* **Swagger UI URL**：`http://localhost:8080/swagger-ui/index.html`
* **OpenAPI 規範 JSON**：`http://localhost:8080/v3/api-docs`
* **測試步驟**：
  1. 展開 **`範本上傳檢核 API`** ➔ `POST /api/v1/upload`。
  2. 點擊右上角 **Try it out**。
  3. `resource` 參數中，將 `"name"` 填入 `"PO_IMPORT"`、`"type"` 填入 `"PO"`。
  4. `file` 參數點擊選擇檔案，選取本機之 `src/main/resources/templates/PO_IMPORT.xlsx`。
  5. 點擊 **Execute** 執行，即可在下方直接觀察 `HTTP 422 Unprocessable Entity` 與精準儲存格座標（如 `D2`、`D7` 等）的檢核失敗訊息。

---

#### 4. API 回應結構規格 (Response Specifications)

##### (1) HTTP 200 OK（驗證全數通過且上傳成功）
```json
{
  "code": "200",
  "message": "上傳成功"
}
```

##### (2) HTTP 422 Unprocessable Entity（Excel 商業規則檢核未通過）
當上傳之 Excel 試算表內容存在違規時，由全域例外處理器攔截並回傳標準之 [BaseExceptionsResponse](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/iface/resource/out/BaseExceptionsResponse.java)：
```json
{
  "code": "VALIDATE_FAILED",
  "messages": [
    "SheetName : 採購訂單明細, D2 資料檢核發生錯誤，欄位 itemNo 的值 'MAT-9001' 重複",
    "SheetName : 採購訂單明細, D7 資料檢核發生錯誤，欄位 itemNo 的值 'MAT-9001' 重複",
    "SheetName = 採購訂單明細, F3 [採購數量] 必須為大於 0 的正整數，當前值為 [-2]",
    "SheetName = 採購訂單明細, C4 [供應商代碼] [V9999] 不存在或非核准合格供應商",
    "SheetName = 採購訂單明細, J5 [約定交期] 格式不合規，需為 YYYY-MM-DD (例如 2026-12-31)",
    "SheetName = 採購訂單明細, H6 [採購單價] 必須為大於 0 的有效數值，當前值為 [0]"
  ]
}
```

##### (3) HTTP 422 Unprocessable Entity（缺少必備工作表或必備標題）
* 若缺少「採購訂單明細」頁籤：
  ```json
  {
    "code": "VALIDATE_FAILED",
    "messages": [
      "缺少必要工作表：【採購訂單明細】(預期工作表代碼: PO_ITEM)"
    ]
  }
  ```
* 若缺少「採購單價」或「採購單號」標題：
  ```json
  {
    "code": "VALIDATE_FAILED",
    "messages": [
      "工作表 [PO_ITEM] 缺少必備欄位標題: 採購單價 (代碼: unitPrice)"
    ]
  }
  ```

---

### 5.4 批次檔案與物件儲存串流處理適配器 (Batch File & Stream Processor with @CustomValidate)

除了 REST API 之外，六角形架構支援非 HTTP 管道的批次處理（如自 MinIO / S3 物件儲存拉取串流、SFTP 檔案或批次任務）。系統透過抽象介面 [BatchFileProcessor](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/iface/schedule/processor/BatchFileProcessor.java) 與具體實作 [PoImportFileProcessor](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/iface/schedule/processor/PoImportFileProcessor.java)，提供高度彈性且支援二進位串流的宣告式檢核架構：

* **核心抽象與處理器實作**：
  * **抽象合約 [BatchFileProcessor](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/iface/schedule/processor/BatchFileProcessor.java)**：定義 `getTemplateName()`、`supports(File)`，並在介面方法宣告式掛載檢核合約：
    * `void processFile(File excelFile)`：支援實體檔案路徑處理。
    * `void processStream(InputStream inputStream)`：支援 MinIO / S3 物件儲存直接讀取之輸入串流，免除落地硬碟暫存之 I/O 負擔。
  * **採購匯入適配器 [PoImportFileProcessor](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/iface/schedule/processor/PoImportFileProcessor.java)**：實作 `BatchFileProcessor`，專注於組裝採購命令並調用核心應用層 [UploadCommandService](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/application/service/UploadCommandService.java)。

* **MinIO / S3 串流與檔案處理流程**：
  1. **獲取串流或檔案**：外部排程或事件監聽器自 MinIO (`minioClient.getObject(...)`) 或檔案系統取得 Excel 資料。
  2. **宣告式校驗**：呼叫 `processor.processStream(inputStream)` 或 `processor.processFile(file)`。
  3. **AOP 動態攔截**：[CustomValidationAspect](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/iface/aspect/CustomValidationAspect.java) 自動提取傳入之串流或檔案，調用 [ValidateExcelUseCase](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/application/port/in/ValidateExcelUseCase.java) 進行校驗。
  4. **錯誤攔截**：若檢核不通過，切面拋出 `ExcelValidationException`，阻擋核心應用層入庫。
  5. **入庫執行**：檢核通過後，自動將可重讀之串流傳遞至應用層 [UploadCommandService](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/application/service/UploadCommandService.java)，達成純淨架構。

#### 程式碼串接範例 (MinIO 物件儲存整合)
```java
@Service
@RequiredArgsConstructor
public class PoMinioImportService {

    private final MinioClient minioClient;
    private final PoImportFileProcessor poImportFileProcessor;

    public void importFromBucket(String bucketName, String objectName) throws Exception {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucketName).object(objectName).build())) {
            
            // 直接傳遞 InputStream，由 @CustomValidate 進行驗證，無需落地暫存至本機磁碟
            poImportFileProcessor.processStream(stream);
        }
    }
}
```

---

## 六、 第四部分：端到端測試案例與錯誤呈現 (Verification & Error Samples)

實體測試檔案已放置於：[src/main/resources/templates/PO_IMPORT.xlsx](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/resources/templates/PO_IMPORT.xlsx)。

### 6.1 測試檔案範例內容 (Sample Excel Data)

| 行號 | 採購單號 | 項次 | 供應商代碼 | 物料料號 | 物料品名 | 採購數量 | 計量單位 | 採購單價 | 幣別 | 約定交期 | 備註 | 測試設計情境 |
| :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: | :---: | :---: | :---: | :--- | :--- |
| **Row 2** | `PO-2026-0001` | `10` | `V1001` | `MAT-9001` | 工業伺服馬達 | `5` | `SET` | `45000` | `TWD` | `2026-11-30` | 生產線核心元件 | ✅ 正確基準資料筆 |
| **Row 3** | `PO-2026-0001` | `20` | `V1001` | `MAT-9002` | 變頻器控制器 | **`-2`** | `PC` | `18000` | `TWD` | `2026-11-30` | 備用件 | ❌ 採購數量為負數 (需 > 0) |
| **Row 4** | `PO-2026-0001` | `30` | **`V9999`** | `MAT-9003` | 感測傳導線纜 | `10` | `M` | `500` | `TWD` | `2026-12-05` | 非合格廠商 | ❌ 供應商代碼不存在 |
| **Row 5** | `PO-2026-0002` | `10` | `V1002` | `MAT-9004` | 可程式邏輯控制器 | `3` | `SET` | `88000` | `USD` | **`2026/12/10`** | 進口設備 | ❌ 約定交期格式非 YYYY-MM-DD |
| **Row 6** | `PO-2026-0002` | `20` | `V1002` | `MAT-9005` | 安全急停開關 | `8` | `PC` | **`0`** | `USD` | `2026-12-10` | 報價異常 | ❌ 採購單價為 0 (需 > 0) |
| **Row 7** | `PO-2026-0003` | `10` | `V2001` | **`MAT-9001`** | 工業伺服馬達 (重複) | `2` | `SET` | `45000` | `TWD` | `2026-12-15` | 重複申購 | ❌ 與 Row 2 料號重複 |

---

### 6.2 測試情境對照矩陣 (BDD Gherkin Test Scenarios)

專案中所有測試案例皆採用 **BDD Gherkin 語法（Feature, Scenario, Given-When-Then）** 進行精確情境定義與驗證，全數收錄於測試類別中：

#### 1. 雙層防腐與 SpEL 規則整合測試：[PoImportValidateTest.java](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/test/java/com/example/demo/service/PoImportValidateTest.java)

| 場景編號 | 測試方法 | 情境描述 (Feature & Scenario) | 預期結果 (Then) |
| :---: | :--- | :--- | :--- |
| **場景 1** | `testValidPoImportData` | **合規採購單全鏈路檢核**：傳入完全合規之 PO Excel 資料（單號、項次、合格廠商、品項、數量、單位、單價、幣別、交期） | 雙層正規化成功，客製檢核錯誤清單為空（`errors.isEmpty() == true`）。 |
| **場景 2** | `testMissingRequiredSheet` | **Level 1 工作表防腐檢核**：上傳試算表缺少必備頁籤「採購訂單明細」 | 立即拋出 `ExcelValidationException`，錯誤訊息提示 `缺少必要工作表：【採購訂單明細】`。 |
| **場景 3** | `testMissingRequiredHeaders` | **Level 2 標題防腐檢核**：首列標題缺漏必備欄位「採購單價」 | 在第 0 列攔截並拋出 `ExcelValidationException`，錯誤訊息明確提示缺少【採購單價】標題。 |
| **場景 4** | `testInvalidRowBusinessPolicies` | **商業規則細部邊界檢核**：資料包含單號格式錯誤、非核准廠商、數量為0、項次負數、單位非法、單價負數、幣別不支援、交期格式錯誤 | 規則引擎精準捕獲 7 項以上之明確違規，並給予具體儲存格錯誤訊息。 |
| **場景 5** | `testGenerateAndValidatePoImportExcelFile` | **實體 Excel 範本生成與全鏈路驗證**：於 `templates/PO_IMPORT.xlsx` 生成實體試算表並以 `ExcelUtil` 讀回驗證 | 檔案成功生成，全鏈路檢核精確捕獲 6 處違規（數量、供應商、交期、單價各 1 筆，料號重複通報 2 筆）。 |

#### 2. 批次處理器與切面測試：[PoImportFileProcessorTest.java](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/test/java/com/example/demo/iface/schedule/processor/PoImportFileProcessorTest.java)

| 場景編號 | 測試方法 | 情境描述 (Feature & Scenario) | 預期結果 (Then) |
| :---: | :--- | :--- | :--- |
| **場景 6** | `processFile_shouldSucceed_whenValidExcelFile` | **實體檔案批次入庫**：傳入合法之本機採購訂單 Excel 檔案 | 通過 `@CustomValidate` 切面檢核，調用應用層 `uploadCommandService.upload`。 |
| **場景 7** | `processFile_shouldThrowException_whenValidationFails` | **實體檔案校驗阻擋**：傳入資料不合規之本機採購訂單 Excel 檔案 | AOP 切面攔截並拋出 `ExcelValidationException`，應用層入庫方法未被執行 (`never()`)。 |
| **場景 8** | `processStream_shouldSucceed_whenValidStream` | **MinIO / S3 串流批次入庫**：自物件儲存讀取合規模擬之二進位串流 | 通過 `@CustomValidate` 切面檢核，可重讀串流順利抵達應用層完成入庫。 |
| **場景 9** | `processStream_shouldThrowException_whenValidationFails` | **MinIO / S3 串流校驗阻擋**：自物件儲存讀取不合規之二進位串流 | AOP 切面攔截並拋出 `ExcelValidationException`，應用層入庫方法未被執行。 |
| **場景 10** | `supports_shouldFilterByPrefixAndExtension` | **適配過濾器多型匹配**：傳入以 `PO` 開頭之檔案、其他副檔名與非 PO 檔案 | 僅有合規之 PO 試算表（不分大小寫）回傳 `true`，其餘回傳 `false`。 |

---

### 6.3 檢核失敗時 API 回傳之 JSON 結構

當表現層（如 `POST /api/v1/upload`）遭遇檢核失敗時，由 [GlobalExceptionHandler](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/iface/advice/GlobalExceptionHandler.java) 統一攔截並轉譯為標準 HTTP 422 結構：

```json
{
  "code": "VALIDATE_FAILED",
  "messages": [
    "SheetName : 採購訂單明細, D2 資料檢核發生錯誤，欄位 itemNo 的值 'MAT-9001' 重複",
    "SheetName : 採購訂單明細, D7 資料檢核發生錯誤，欄位 itemNo 的值 'MAT-9001' 重複",
    "SheetName = 採購訂單明細, F3 [採購數量] 必須為大於 0 的正整數，當前值為 [-2]",
    "SheetName = 採購訂單明細, C4 [供應商代碼] [V9999] 不存在或非核准合格供應商",
    "SheetName = 採購訂單明細, J5 [約定交期] 格式不合規，需為 YYYY-MM-DD (例如 2026-12-31)",
    "SheetName = 採購訂單明細, H6 [採購單價] 必須為大於 0 的有效數值，當前值為 [0]"
  ]
}
```

---

## 七、 第五部分：總結與最佳實踐建議 (Best Practices)

1. **標題順序與命名彈性**：
   - 透過 Level 2 防腐對映，業務上傳 Excel 的欄位順序無論如何調整、或將表頭改為別名，只要有在 `TemplateFieldMapping` 中註冊，系統均可自動正確對齊。
2. **多枚舉清單變數之外部化維護**：
   - 採購單中的合格供應商（`#validVendors`）、核准幣別（`#validCurrencies`）與計量單位（`#validUnits`）皆透過 `rule = 'VARIABLE'` 策略外部化注入，未來調整名單無需重新編譯 Java 專案。
3. **雲原生 MinIO / S3 串流零落地優化**：
   - 批次處理器原生支援 `processStream(InputStream)`，在 Kubernetes 容器化部署環境下無需掛載本機暫存磁區，切面自動透過記憶體重設串流保證應用層可重複讀取。
4. **持續整合與測試保護**：
   - 本規格已完整落實於 [PoImportValidateTest.java](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/test/java/com/example/demo/service/PoImportValidateTest.java) 與 [PoImportFileProcessorTest.java](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/test/java/com/example/demo/iface/schedule/processor/PoImportFileProcessorTest.java)，所有規則與情境均有單元測試防護（`BUILD SUCCESS`），確保後續系統演進時商業規則不被破壞。
