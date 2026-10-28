# PR Import (請購單批次匯入) 客製化驗證實作指南

> **文件版本**：v1.0.0 (正式發布版)  
> **更新日期**：2026-10-28  
> **適用範本**：`PR_IMPORT` (Purchase Requisition Batch Import)  
> **依循架構規範**：[AGENTS.md](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/.agents/AGENTS.md) (DDD / Clean Architecture / 雙層防腐層 / SpEL 驗證核心)

---

## 一、 業務情境與核心設計目標 (Overview & Goals)

### 1.1 業務背景 (Business Scenario)
在企業採購流程中，**PR（Purchase Requisition，請購單）** 是各部門發起採購需求的前置單據。業務或專案單位透過 Excel 試算表批次匯入請購資料，系統需確保上傳內容在進入審核流程並轉為 **PO（Purchase Order，採購單）** 之前，其欄位結構、必填性、資料型別與商業規則皆正確無誤。

### 1.2 核心痛點與防腐設計目標
1. **業務語言與系統代碼解耦 (Decoupling Business & System Names)**：
   - 業務人員使用直觀的中文工作表名稱（如「請購明細清單」）與中文標題（如「物料料號」、「需求數量」）。
   - 後端領域模型與 DTO 統一使用標準代碼（如 `PR_ITEM`、`itemNo`、`quantity`）。
2. **兩階段快速失敗 (Two-Phase Fail-Fast)**：
   - **第一層 (Level 1)**：在解析資料列前，先檢驗必備工作表是否存在，缺少即刻中斷。
   - **第二層 (Level 2)**：在計算商業規則前，先檢驗各工作表必備欄位標題是否存在，缺少即刻中斷。
3. **動態且非侵入性的商業規則 (Configurable Business Policies via SpEL)**：
   - 透過 Spring Expression Language (SpEL)，將必填、邊界值判斷、格式正則、代碼清單合法性與整表唯一性規則外部化配置，無需修改任何後端 Java 程式碼即可彈性調整。

---

## 二、 雙層防腐架構與資料流 (Two-Tier Architecture Data Flow)

```mermaid
flowchart TD
    subgraph Client_Layer [表現層 / Client]
        A["業務上傳 Excel 檔案<br>工作表: 【請購明細清單】<br>中文標題: 物料料號, 物料品名, 規格型號, 需求數量, 預算單價, 成本中心, 需求交期"]
    end

    subgraph Infrastructure_Adapter [基礎設施適配器 / ExcelValidatorAdapter]
        B["ExcelUtil.readAllSheets(InputStream)<br>解構為原始多工作表 Map 矩陣"]
        
        subgraph Level_1_ACL [Level 1 ACL: 工作表防腐層 (TemplateSheetNormalizer)]
            C1{"核對必備工作表<br>(isRequired == Y)"}
            C2["頁籤名稱正規化:<br>「請購明細清單」 ➔ PR_ITEM"]
        end
        
        subgraph Level_2_ACL [Level 2 ACL: 欄位標題防腐層 (TemplateHeaderNormalizer)]
            D1{"核對必備欄位標題<br>(isRequired == Y)"}
            D2["標題代碼正規化:<br>中文標題 ➔ 內部 CamelCase 屬性代碼<br>(物料料號 ➔ itemNo, 需求數量 ➔ quantity...)"]
        end
    end

    subgraph Infrastructure_CV_Engine [客製驗證核心 / CustomValidator]
        E["ContextRoot 上下文組裝<br>綁定 normalizedExcelData 與 sheetDisplayNameMap"]
        F1["SpEL 變數注入: #validCostCenters 清單"]
        F2["SpEL 整表檢核: #validateDuplicate(#sheet, 'itemNo')"]
        F3["SpEL 逐列檢核: 必填、正整數、正數單價、日期正則、成本中心存在性"]
    end

    subgraph Exit_Status [出口狀態]
        G1["【校驗失敗】<br>統一拋出 ExcelValidationException<br>由 GlobalExceptionHandler 轉為 HTTP 422 JSON"]
        G2["【校驗成功】<br>產出乾淨之標準結構化資料<br>下游一鍵轉換為 PR 業務 DTO / 草稿單"]
    end

    A --> B
    B --> C1
    C1 -- "缺漏必備工作表" --> G1
    C1 -- "通過" --> C2
    C2 --> D1
    D1 -- "缺漏必備欄位標題" --> G1
    D1 -- "通過" --> D2
    D2 --> E
    E --> F1 --> F2 --> F3
    F3 -- "存在資料違規" --> G1
    F3 -- "全數合規" --> G2
```

---

## 三、 第一部分：範本對映設定 (Template Mapping Specification)

範本唯一識別名稱代碼為：`PR_IMPORT`。

### 3.1 Level 1：工作表層級對映設定 (`TemplateSheetMapping`)
負責外部 Excel 實體工作表（Sheet）名稱與系統內部標準工作表代碼的防腐對齊。

| 屬性欄位 | 設定值 | 說明 |
| :--- | :--- | :--- |
| **範本唯一名稱 (`templateName`)** | `PR_IMPORT` | 範本唯一識別碼 |
| **內部工作表代碼 (`sheetCode`)** | `PR_ITEM` | 系統內部業務統一代碼 |
| **外部顯示名稱 (`displayName`)** | `請購明細清單` | 外部 Excel 實體頁籤名稱（支援雙向別名相容，填寫 `PR_ITEM` 或 `請購明細清單` 均可辨識） |
| **預期工作表索引 (`sheetIndex`)** | `0` | 首頁工作表（0-based，非強制） |
| **是否必備 (`isRequired`)** | `Y` | 前置防呆：缺少此工作表立即中斷並拋錯 |
| **啟用註記 (`activeFlag`)** | `Y` | 是否啟用此對映 |

---

### 3.2 Level 2：欄位標題層級對映設定 (`TemplateFieldMapping`)
負責外部 Excel 首列實體中文標題與系統內部屬性鍵值（CamelCase）的對齊與順序規範。

| 實體中文標題 (`displayName`) | 內部屬性代碼 (`fieldCode`) | 預期順序 (`columnIndex`) | 是否必填標題 (`isRequired`) | 資料型別 (`dataType`) | 說明 |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **物料料號** | `itemNo` | `1` | `Y` | `STRING` | 採購料號，同單不重複 |
| **物料品名** | `itemName` | `2` | `Y` | `STRING` | 物料中文名稱 |
| **規格型號** | `spec` | `3` | `N` | `STRING` | 詳細規格描述（非必備欄位） |
| **需求數量** | `quantity` | `4` | `Y` | `NUMBER` | 採購數量（必須為正整數） |
| **預算單價** | `unitPrice` | `5` | `Y` | `NUMBER` | 預估單價（必須為正數） |
| **成本中心** | `costCenter` | `6` | `Y` | `STRING` | 歸屬部門或成本中心代碼 |
| **需求交期** | `requiredDate` | `7` | `Y` | `DATE` | 期望到貨日期 (`YYYY-MM-DD`) |

> **防腐效益**：
> 1. 上傳若缺少標記 `isRequired = Y` 的任何欄位標題（例如誤刪「預算單價」），第二層防腐層將在**第 0 列**直接攔截，防止後續資料列解析產生非預期的 `NullPointerException`。
> 2. 後續的所有檢核規則（Validation Policy）與應用層 DTO 轉換，均能基於統一的 CamelCase 屬性代碼（如 `itemNo`, `quantity`）運作，免除繁瑣的手動轉對映邏輯。

---

## 四、 第二部分：客製檢核規則設定 (Validation Policy Specification)

透過 SpEL 動態表達式，設定對應的資料約束。所有規則儲存於策略設定清單中。

### 4.1 檢核原則清單全貌

| 優先序 | 類型 (`type`) | 檢核欄位 (`mappingFieldName`) | 規則代碼 (`rule`) | SpEL 表達式 (`expression`) | 錯誤訊息模板 (`errorMessage`) | 業務約束說明 |
| :---: | :---: | :--- | :--- | :--- | :--- | :--- |
| **1** | `SHEET` | `validCostCenters` | `VARIABLE` | `{'CC001', 'CC002', 'CC003', 'IT001', 'HR001'}` | *(無)* | **全域環境變數注入**：宣告核准之成本中心合法清單 |
| **2** | `SHEET` | `itemNo` | `VALIDATE_DUPLICATE` | `#validateDuplicate(#sheet, 'itemNo')` | *(由函式動態產出)* | **整表唯一性檢核**：同張請購單內物料料號不可重複 |
| **3** | `ROW` | `itemNo` | `ENFORCE_ROW_VALIDATION` | `#checkedValue != null && #checkedValue.trim() != ''` | `'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [物料料號] 為必填項目，不可為空'` | **必填檢查**：料號不得為空 |
| **4** | `ROW` | `itemName` | `ENFORCE_ROW_VALIDATION` | `#checkedValue != null && #checkedValue.trim() != ''` | `'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [物料品名] 為必填項目，不可為空'` | **必填檢查**：品名不得為空 |
| **5** | `ROW` | `quantity` | `ENFORCE_ROW_VALIDATION` | `#isNumeric(#checkedValue) && T(java.lang.Integer).parseInt(#checkedValue) > 0` | `'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [需求數量] 必須為大於 0 的正整數，當前值為 [' + #checkedValue + ']'` | **型別與邊界**：數量必須為數值且 > 0 |
| **6** | `ROW` | `unitPrice` | `ENFORCE_ROW_VALIDATION` | `#isNumeric(#checkedValue) && T(java.lang.Double).parseDouble(#checkedValue) > 0` | `'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [預算單價] 必須為大於 0 的有效數值，當前值為 [' + #checkedValue + ']'` | **型別與邊界**：單價必須為數值且 > 0 |
| **7** | `ROW` | `costCenter` | `ENFORCE_ROW_VALIDATION` | `#validCostCenters.contains(#checkedValue)` | `'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [成本中心] 代碼 [' + #checkedValue + '] 不存在或非有效清單'` | **代碼合法性**：比對規則 1 所注入的變數清單 |
| **8** | `ROW` | `requiredDate` | `ENFORCE_ROW_VALIDATION` | `#checkedValue != null && #checkedValue.matches('^\\d{4}-\\d{2}-\\d{2}$')` | `'SheetName = ' + #sheetName + ', ' + #excelAddress + ' [需求交期] 格式不合規，需為 YYYY-MM-DD (例如 2026-12-31)'` | **格式檢核**：日期必須嚴格符合 ISO 格式 |

---

### 4.2 SpEL 核心語法與變數對照表

檢核引擎（[CustomValidator](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/infra/cv/validator/CustomValidator.java)）在評估表達式時，已預先注入以下上下文變數與內建工具函式：

| 變數 / 函式 | 說明與取用範例 |
| :--- | :--- |
| **`#checkedValue`** | 當前評估之儲存格字串值（如 `"3"`、`"IT001"`）。 |
| **`#sheet`** | 當前工作表之所有資料列集合 (`List<Map<String, String>>`)。 |
| **`#sheetName`** | 當前工作表外部友善顯示名稱（如 `"請購明細清單"`）。 |
| **`#excelAddress`** | 實體 Excel 座標字串（如 `"A2"`、`"D3"`），由列號與欄號動態計算生成。 |
| **`#isNumeric(...)`** | 內建靜態驗證函式，判定是否為合法數值。 |
| **`#validateDuplicate(...)`** | 內建靜態驗證函式，檢查指定欄位是否重複，回傳違規行號與訊息。 |
| **`#contains(...)`** | 內建靜態驗證函式，檢查工作表指定欄位值是否存在於限定集合中。 |
| **自訂全域變數** | 透過 `rule = 'VARIABLE'` 策略設定的變數名稱，如 `#validCostCenters`。 |

---

## 五、 第三部分：程式碼串接與執行流程 (Code Integration Walkthrough)

### 5.1 呼叫輸出適配器 (`ExcelValidatorPort`)

在應用層 UseCase 或 Service 中，調用六角形架構的 Outbound Port 介面執行驗證：

```java
@Service
public class UploadPrApplicationService {

    private final ExcelValidatorPort excelValidatorPort;

    public UploadPrApplicationService(ExcelValidatorPort excelValidatorPort) {
        this.excelValidatorPort = excelValidatorPort;
    }

    public void execute(InputStream inputStream) {
        // 一行指令完成雙層防腐正規化與全套業務規則檢核
        // 若有任何缺漏或資料違規，將自動拋出 ExcelValidationException
        excelValidatorPort.validateExcelData("PR_IMPORT", inputStream);
        
        // 通過檢核後，執行後續 PR 請購單領域物件建立與持久化...
    }
}
```

### 5.2 內部適配器執行步驟分析 (`ExcelValidatorAdapter`)
適配器內部按照兩階段快速失敗與防腐機制協調整合：
1. **載入策略定義**：自倉儲取得 `PR_IMPORT` 的 `TemplateSheetMapping`、`TemplateFieldMapping` 與 `ValidationPolicy`。
2. **解構工作簿**：透過 `ExcelUtil.readAllSheets(inputStream)` 取得所有頁籤原始資料。
3. **Level 1 防腐**：調用 `templateSheetNormalizer.normalize(...)`。若缺少「請購明細清單」頁籤立即拋出 `SHEET_VALIDATION_FAILED`。
4. **Level 2 防腐**：調用 `templateHeaderNormalizer.normalize(...)`。若缺少「物料料號」、「需求數量」等必填標題立即拋出 `HEADER_VALIDATION_FAILED`。
5. **引擎運算**：組裝 `ContextRoot`，調用 `customValidator.validateExcelData(...)` 進行 SpEL 規則運算。
6. **錯誤收斂**：若產生任何 `ValidationError`，封裝並拋出結構化 `ExcelValidationException`。

---

### 5.3 表現層上傳 REST API 呼叫規格 (Upload API Specification & Request Data)

系統提供標準 Multipart 檔案上傳 API 端點，支援前端或第三方系統將 PR Excel 檔案與中繼資訊一同上傳並觸發校驗：

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
| **`file`** | 二進位檔案 | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` | **是** | 待檢核之 Excel 檔案（如 `PR_IMPORT.xlsx`） |

##### `resource` 參數 JSON 結構明細：
```json
{
  "name": "PR_IMPORT",
  "type": "PR",
  "fileType": "xlsx",
  "filePath": "/TEMPLATE/PR_IMPORT",
  "fileName": "PR_IMPORT.xlsx"
}
```

* **欄位校驗規則**：
  * `name`：範本唯一識別碼，**必填**，對應 `validation_policy` 與 `template_field_mapping` 的 `template_name`。
  * `type`：範本種類代碼，**必填**（如 `"PR"`、`"EXCEL"`）。
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
    "name": "PR_IMPORT",
    "type": "PR",
    "fileType": "xlsx",
    "filePath": "/TEMPLATE/PR_IMPORT",
    "fileName": "PR_IMPORT.xlsx"
  };type=application/json' \
  -F "file=@d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/resources/templates/PR_IMPORT.xlsx"
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
       "name": "PR_IMPORT",
       "type": "PR",
       "fileType": "xlsx",
       "filePath": "/TEMPLATE/PR_IMPORT",
       "fileName": "PR_IMPORT.xlsx"
     }
     ```
4. 新增欄位 2：
   * Key：`file`
   * Type：選擇 `File`
   * Value：選擇本機的 `PR_IMPORT.xlsx` 檔案。

##### (3) 前端 JavaScript / Fetch API 範例
```javascript
const uploadPrExcel = async (fileBlob) => {
  const formData = new FormData();

  // 1. 組裝 resource JSON Part
  const resourceData = {
    name: "PR_IMPORT",
    type: "PR",
    fileType: "xlsx",
    filePath: "/TEMPLATE/PR_IMPORT",
    fileName: "PR_IMPORT.xlsx"
  };
  formData.append("resource", new Blob([JSON.stringify(resourceData)], { 
    type: "application/json" 
  }));

  // 2. 組裝 Excel File Part
  formData.append("file", fileBlob, "PR_IMPORT.xlsx");

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
專案已整合 OpenAPI 3.0 與 Swagger UI，並將 `PR_IMPORT` 設定為中繼資訊預設範例值：
* **Swagger UI URL**：`http://localhost:8080/swagger-ui/index.html`
* **OpenAPI 規範 JSON**：`http://localhost:8080/v3/api-docs`
* **測試步驟**：
  1. 展開 **`範本上傳檢核 API`** ➔ `POST /api/v1/upload`。
  2. 點擊右上角 **Try it out**。
  3. `resource` 參數已自動帶入 `PR_IMPORT` 的範例 JSON 結構。
  4. `file` 參數點擊選擇檔案，選取本機之 `src/main/resources/templates/PR_IMPORT.xlsx`。
  5. 點擊 **Execute** 執行，即可在下方直接觀察 `HTTP 422 Unprocessable Entity` 與精準儲存格座標（`A2`、`A7`）的檢核失敗訊息。

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
    "SheetName : 請購明細清單, A2 資料檢核發生錯誤，欄位 itemNo 的值 'MAT-1001' 重複",
    "SheetName : 請購明細清單, A7 資料檢核發生錯誤，欄位 itemNo 的值 'MAT-1001' 重複",
    "SheetName = 請購明細清單, D3 [需求數量] 必須為大於 0 的正整數，當前值為 [-5]",
    "SheetName = 請購明細清單, E6 [預算單價] 必須為大於 0 的有效數值，當前值為 [0]",
    "SheetName = 請購明細清單, F4 [成本中心] 代碼 [SALES999] 不存在或非有效清單",
    "SheetName = 請購明細清單, G5 [需求交期] 格式不合規，需為 YYYY-MM-DD (例如 2026-12-31)"
  ]
}
```

##### (3) HTTP 422 Unprocessable Entity（缺少必備工作表或必備標題）
* 若缺少「請購明細清單」頁籤：
  ```json
  {
    "code": "VALIDATE_FAILED",
    "messages": [
      "範本 [PR_IMPORT] 缺少必要工作表: 請購明細清單 (代碼: PR_ITEM)"
    ]
  }
  ```
* 若缺少「物料料號」或「預算單價」標題：
  ```json
  {
    "code": "VALIDATE_FAILED",
    "messages": [
      "工作表 [PR_ITEM] 缺少必備欄位標題: 預算單價 (代碼: unitPrice)"
    ]
  }
  ```

---

#### 5. 批次檔案與物件儲存串流處理適配器 (Batch File & Stream Processor with @CustomValidate)

除了 REST API 之外，六角形架構支援非 HTTP 管道的批次處理（如自 MinIO / S3 物件儲存拉取串流、SFTP 檔案或批次任務）。系統透過抽象介面 [BatchFileProcessor](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/iface/schedule/processor/BatchFileProcessor.java) 與具體實作 [PrImportFileProcessor](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/iface/schedule/processor/PrImportFileProcessor.java)，提供高度彈性且支援二進位串流的宣告式檢核架構：

* **核心抽象與處理器實作**：
  * **抽象合約 [BatchFileProcessor](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/iface/schedule/processor/BatchFileProcessor.java)**：定義 `getTemplateName()`、`supports(File)`，並在介面方法宣告式掛載檢核合約：
    * `void processFile(File excelFile)`：支援實體檔案路徑處理。
    * `void processStream(InputStream inputStream)`：支援 MinIO / S3 物件儲存直接讀取之輸入串流，免除落地硬碟暫存之 I/O 負擔。
  * **請購匯入適配器 [PrImportFileProcessor](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/iface/schedule/processor/PrImportFileProcessor.java)**：實作 `BatchFileProcessor`，專注於組裝請購命令並調用核心應用層 [UploadCommandService](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/application/service/UploadCommandService.java)。

* **MinIO / S3 串流與檔案處理流程**：
  1. **獲取串流或檔案**：外部排程或事件監聽器自 MinIO (`minioClient.getObject(...)`) 或檔案系統取得 Excel 資料。
  2. **宣告式校驗**：呼叫 `processor.processStream(inputStream)` 或 `processor.processFile(file)`。
  3. **AOP 動態攔截**：[CustomValidationAspect](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/iface/aspect/CustomValidationAspect.java) 自動提取傳入之串流或檔案，調用 [ValidateExcelUseCase](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/application/port/in/ValidateExcelUseCase.java) 進行校驗。
  4. **錯誤攔截**：若檢核不通過，切面拋出 `ExcelValidationException`，阻擋核心應用層入庫。
  5. **入庫執行**：檢核通過後，自動將可重讀之串流傳遞至應用層 [UploadCommandService](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/application/service/UploadCommandService.java)，達成純淨架構。

---

## 六、 第四部分：端到端測試案例與錯誤呈現 (Verification & Error Samples)

實體測試檔案已放置於：[src/main/resources/templates/PR_IMPORT.xlsx](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/resources/templates/PR_IMPORT.xlsx)。

### 6.1 測試檔案範例內容

| 行號 | 物料料號 | 物料品名 | 規格型號 | 需求數量 | 預算單價 | 成本中心 | 需求交期 | 測試設計情境 |
| :---: | :--- | :--- | :--- | :---: | :---: | :---: | :---: | :--- |
| **Row 2** | `MAT-1001` | 高效能運算伺服器 | 64-Core / 256GB RAM | `3` | `150000` | `IT001` | `2026-11-30` | ✅ 正確基準資料筆 |
| **Row 3** | `MAT-1002` | 人體工學升降桌 | 雙馬達 160x80cm | **`-5`** | `12000` | `HR001` | `2026-12-15` | ❌ 數量為負數 (需 > 0) |
| **Row 4** | `MAT-1003` | 彩色雷射印表機 | A4/A3 雙面彩色列印 | `2` | `35000` | **`SALES999`** | `2026-12-20` | ❌ 成本中心代碼不存在 |
| **Row 5** | `MAT-1004` | 網路交換器 | 24-Port Gigabit PoE | `4` | `18000` | `CC001` | **`2026/11/25`** | ❌ 交期格式非 YYYY-MM-DD |
| **Row 6** | `MAT-1005` | 無線會議麥克風 | 一對二無線藍牙款 | `1` | **`0`** | `CC002` | `2026-12-31` | ❌ 單價為 0 (需 > 0) |
| **Row 7** | **`MAT-1001`** | 高效能運算伺服器 (加購) | 同上規格 | `2` | `150000` | `IT001` | `2026-12-10` | ❌ 與 Row 2 料號重複 |

---

### 6.2 檢核失敗時 API 回傳之 JSON 結構

當表現層（如 `POST /api/v1/upload`）遭遇檢核失敗時，由 [GlobalExceptionHandler](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/iface/advice/GlobalExceptionHandler.java) 統一攔截並轉譯為標準 HTTP 422 結構：

```json
{
  "code": "VALIDATE_FAILED",
  "messages": [
    "SheetName : 請購明細清單, A2 資料檢核發生錯誤，欄位 itemNo 的值 'MAT-1001' 重複",
    "SheetName : 請購明細清單, A7 資料檢核發生錯誤，欄位 itemNo 的值 'MAT-1001' 重複",
    "SheetName = 請購明細清單, D3 [需求數量] 必須為大於 0 的正整數，當前值為 [-5]",
    "SheetName = 請購明細清單, E6 [預算單價] 必須為大於 0 的有效數值，當前值為 [0]",
    "SheetName = 請購明細清單, F4 [成本中心] 代碼 [SALES999] 不存在或非有效清單",
    "SheetName = 請購明細清單, G5 [需求交期] 格式不合規，需為 YYYY-MM-DD (例如 2026-12-31)"
  ]
}
```

---

## 七、 總結與最佳實踐建議 (Best Practices)

1. **標題順序與命名彈性**：
   - 透過 Level 2 防腐對映，業務上傳 Excel 的欄位順序無論如何調整、或將表頭改為別名，只要有在 `TemplateFieldMapping` 中註冊，系統均可自動正確對齊。
2. **非必填欄位的空值安全**：
   - 對於非必填欄位（未設定 `ENFORCE_ROW_VALIDATION`），當儲存格為空或空白時，引擎會自動跳過運算，避免觸發誤報。
3. **持續整合與測試保護**：
   - 本規格已落實於 [PrImportValidateTest.java](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/test/java/com/example/demo/service/PrImportValidateTest.java)，所有規則與情境均有單元測試防護（`BUILD SUCCESS`），確保後續系統演進時商業規則不被破壞。
