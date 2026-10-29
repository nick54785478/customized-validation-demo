# 客製化驗證模組 (Customized Validation Module)

[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Clean Architecture](https://img.shields.io/badge/Architecture-Clean%20%2F%20Hexagonal-blue.svg)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0%20(SpringDoc)-green.svg)](http://localhost:8080/swagger-ui.html)
[![Build Status](https://img.shields.io/badge/Build-Passing%20(81%2F81%20Tests)-success.svg)]()

`customized-validation-module` 是一個專為企業級應用設計的**通用 Excel 宣告式資料驗證與防腐中介模組**。本模組嚴格遵循 **Clean Architecture（六角形架構）** 與 **DDD（領域驅動設計）** 原則，徹底消除外部技術框架對核心業務層的侵入，並提供可高度擴充的**雙層範本對映機制（Two-Tier Template Mapping）**與**宣告式 `@CustomValidate` AOP 驗證切面**。

無論是透過 **Web REST API 上傳**，或是透過 **MinIO / S3 物件儲存串流** 與本機/SFTP 檔案進行批次排程處理，皆能以極低代碼侵入度無縫接入嚴密的格式與商業邏輯檢核。

---

## 📑 目錄

- [一、 核心架構與設計原則](#一-核心架構與設計原則)
- [二、 核心功能特色](#二-核心功能特色)
  - [1. 雙層範本防腐對映 (Two-Tier Template Mapping)](#1-雙層範本防腐對映-two-tier-template-mapping)
  - [2. 宣告式註解驗證機制 (@CustomValidate)](#2-宣告式註解驗證機制-customvalidate)
  - [3. 多端適配處理器 (REST 上傳與 MinIO/S3 串流批次)](#3-多端適配處理器-rest-上傳與-minios3-串流批次)
  - [4. 純資料載體與防腐組裝器 (ACL Assembler)](#4-純資料載體與防腐組裝器-acl-assembler)
- [三、 系統架構與分層目錄](#三-系統架構與分層目錄)
- [四、 快速開始 (Quick Start)](#四-快速開始-quick-start)
  - [環境需求](#環境需求)
  - [資料庫初始化](#資料庫初始化)
  - [構建與執行測試](#構建與執行測試)
  - [啟動應用程式](#啟動應用程式)
- [五、 使用指南與程式碼範例](#五-使用指南與程式碼範例)
  - [1. REST API 上傳檢核](#1-rest-api-上傳檢核)
  - [2. 實作 MinIO / S3 串流批次處理器](#2-實作-minio--s3-串流批次處理器)
  - [3. 雙層對映規則配置 (Sheet & Field Mapping)](#3-雙層對映規則配置-sheet--field-mapping)
- [六、 測試規範與驗證成果](#六-測試規範與驗證成果)
- [七、 相關設計規格文件](#七-相關設計規格文件)

---

## 一、 核心架構與設計原則

本模組嚴格依循專案規範 [AGENTS.md](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/.agents/AGENTS.md) 實作：

```
       +-------------------------------------------------------------+
       |             Presentation Layer (iface / Inbound)            |
       |  UploadController   |   BatchFileProcessor (MinIO/S3/File)  |
       |                   CustomValidationAspect                    |
       +------------------------------+------------------------------+
                                      | calls via Inbound Port
                                      v
       +-------------------------------------------------------------+
       |                    Application Layer                        |
       |   ValidateExcelUseCase   |   UploadCommandService           |
       |   (Pure Java: InputStream / Java 17 Records / DTOs)         |
       +------------------------------+------------------------------+
                                      | calls via Outbound Port
                                      v
       +-------------------------------------------------------------+
       |             Infrastructure Layer (infra / Outbound)         |
       |  ExcelValidatorAdapter   |   TemplateSheetNormalizer (ACL)  |
       |  TemplateHeaderNormalizer|   CustomValidator (Rule Engine)  |
       |  Spring Data JPA Repos   |   Persistence Entities           |
       +-------------------------------------------------------------+
```

1. **六角形架構 Port & Adapter 隔離**：
   - **零技術物件侵入 Port**：所有 Input Port (`port/in/`) 與 Output Port (`port/out/`) 嚴禁接收或回傳 Spring Web `MultipartFile`、JPA 實體或 HTTP 相關物件。統一使用純 Java 串流（`InputStream`）與不可變純資料物件（Java 17 `record`）。
2. **防腐層機制 (Anti-Corruption Layer, ACL)**：
   - 表現層（`iface/`）與應用層（`application/`）間的轉換，由專屬的 `Assembler`（如 [UploadResourceAssembler.java](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/iface/assembler/UploadResourceAssembler.java)）負責，禁止在 DTO 或 Resource 內撰寫轉換邏輯。
3. **模組定位收斂**：
   - 本模組為通用驗證組件，無業務領域聚合根；原本不恰當的 `application.domain` 已徹底退階至基礎設施層（`infra.persistence`），成為驗證引擎讀取的持久化規則模型。

---

## 二、 核心功能特色

### 1. 雙層範本防腐對映 (Two-Tier Template Mapping)

為解決外部 Excel 來源多樣（不同供應商、歷史系統、自訂標籤名稱）所導致的格式變更問題，模組實作了**雙層防腐正規化機制**：

| 層級 | 防腐元件 | 說明 |
| :--- | :--- | :--- |
| **Level 1: Sheet Mapping** | [TemplateSheetNormalizer.java](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/infra/cv/normalizer/TemplateSheetNormalizer.java) | 攔截多樣化的工作表頁籤別名（如「請購明細表」、「PR明細」），自動映射並正規化為標準工作表代碼（如 `PR_ITEM`），並前置檢驗缺少之必要工作表。 |
| **Level 2: Header Mapping** | [TemplateHeaderNormalizer.java](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/infra/cv/normalizer/TemplateHeaderNormalizer.java) | 攔截多變的欄位標題別名（如「請購單號」、「PR單號」），自動正規化為系統內部屬性鍵值（如 `prNo`），並在進入資料檢驗前進行缺失欄位防呆阻擋。 |

### 2. 宣告式註解驗證機制 (`@CustomValidate`)

透過 AOP 註解 [@CustomValidate](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/application/annotation/CustomValidate.java)，以極低耦合度完成 Excel 驗證攔截：

- **動態 SpEL 表達式解析**：支援靜態常數（如 `@CustomValidate(templateName = "PR_IMPORT")`）或動態物件取值（如 `@CustomValidate(templateName = "#resource.type")`、`@CustomValidate(templateName = "#this.templateName")`）。
- **多來源探測**：自動探測方法傳入之 `MultipartFile`、`InputStream`、`File`、`byte[]` 或封裝於 Request/Command Record 內的欄位。
- **串流防護機制**：自動將消耗性之 `InputStream` 轉封裝為可重設之 `ByteArrayInputStream`，確保驗證完成後下游業務服務依然能順暢讀取完整資料。

### 3. 多端適配處理器 (REST 上傳與 MinIO/S3 串流批次)

- **HTTP 表現層**：[UploadController.java](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/iface/rest/UploadController.java) 整合 SpringDoc OpenAPI 3.0，提供圖形化 Swagger UI 上傳體驗。
- **批次處理介面 (Inbound Adapter)**：[BatchFileProcessor.java](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/iface/schedule/processor/BatchFileProcessor.java) 抽離任何特定本機路徑或排程器框架，直接宣告式支援：
  - `processStream(InputStream inputStream)`：**原生支援 MinIO / S3 物件儲存串流**，無需於本機磁碟寫入暫存檔，大幅提升雲原生容器化部署效能。
  - `processFile(File excelFile)`：相容本機磁碟或傳統 SFTP 下載檔案。

### 4. 純資料載體與防腐組裝器 (ACL Assembler)

所有 Request / Response 資源嚴格遵循 AGENTS.md 命名與不可變原則：
- `V + N + Resource`：[UploadTemplateResource.java](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/iface/resource/in/UploadTemplateResource.java)、`CreateTemplateFieldMappingResource` 等。
- `N + Ved + Resource`：[TemplateUploadedResource.java](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/iface/resource/out/TemplateUploadedResource.java)、`TemplateFieldMappingCreatedResource` 等。
- 全數配置 OpenAPI `@Schema` 繁體中文說明與展示值。

---

## 三、 系統架構與分層目錄

```
com.example.demo
├── config                                   # 全域配置層 (OpenAPI 3.0, WebMvc)
│   ├── OpenApiConfig.java
│   └── WebConfig.java
├── iface                                    # 表現層 (Inbound Adapters)
│   ├── advice                               # 全域例外攔截轉譯器
│   │   └── GlobalExceptionHandler.java
│   ├── aspect                               # 註解驅動 AOP 驗證切面
│   │   └── CustomValidationAspect.java
│   ├── assembler                            # 表現層防腐轉換組裝器
│   │   ├── TemplateFieldMappingResourceAssembler.java
│   │   ├── TemplateSheetMappingResourceAssembler.java
│   │   └── UploadResourceAssembler.java
│   ├── resource                             # 表現層純資料載體 (Java 17 Records)
│   │   ├── in                               # Request Resources (V + N + Resource)
│   │   └── out                              # Response Resources (N + Ved + Resource)
│   ├── rest                                 # REST API 控制器 (Upload, Mapping CRUD)
│   │   ├── TemplateFieldMappingController.java
│   │   ├── TemplateSheetMappingController.java
│   │   └── UploadController.java
│   └── schedule                             # 批次匯入適配層 (Inbound Batch Adapters)
│       └── processor
│           ├── BatchFileProcessor.java      # 宣告式批次介面 (支援 File 與 MinIO/S3 串流)
│           ├── PoImportFileProcessor.java   # PO 採購訂單匯入實作處理器
│           └── PrImportFileProcessor.java   # PR 請購匯入實作處理器
├── application                              # 應用核心層 (Application Layer)
│   ├── annotation                           # 驗證宣告註解
│   │   └── CustomValidate.java
│   ├── dto                                  # 應用層傳輸資料載體 (Result Records)
│   ├── exception                            # 應用層業務與檢核例外
│   │   ├── ExcelValidationException.java
│   │   └── ValidationException.java
│   ├── port                                 # 六角形架構連接埠
│   │   ├── in                               # Inbound Ports (Use Cases)
│   │   │   ├── TemplateFieldMappingUseCase.java
│   │   │   ├── TemplateSheetMappingUseCase.java
│   │   │   └── ValidateExcelUseCase.java
│   │   └── out                              # Outbound Ports (Repository / Validator)
│   │       ├── ExcelValidatorPort.java
│   │       ├── TemplateFieldMappingRepositoryPort.java
│   │       └── TemplateSheetMappingRepositoryPort.java
│   ├── service                              # Use Case 實作與流程編排服務
│   │   ├── TemplateFieldMappingApplicationService.java
│   │   ├── TemplateSheetMappingApplicationService.java
│   │   ├── UploadCommandService.java
│   │   └── ValidateExcelApplicationService.java
│   └── shared                               # 跨模組不可變共享物件
│       ├── command                          # 業務命令 (V + N + Command)
│       ├── query                            # 查詢條件 (V + N + Query)
│       └── vo                               # 純值物件 (ValidationError, YesNo)
└── infra                                    # 基礎設施層 (Infrastructure / Outbound Adapters)
    ├── adapter                              # Outbound Port 適配器實作
    │   ├── ExcelValidatorAdapter.java
    │   ├── TemplateFieldMappingRepositoryAdapter.java
    │   └── TemplateSheetMappingRepositoryAdapter.java
    ├── assembler                            # 持久化實體與 DTO 轉換組裝器
    ├── cv                                   # 客製化驗證核心引擎
    │   ├── normalizer                       # 雙層防腐正規化器 (Sheet & Header Normalizer)
    │   ├── parser                           # Excel 單元格解析器
    │   ├── shared                           # 驗證規則列舉與常數
    │   └── validator                        # 規則校驗器 (CustomValidator)
    ├── persistence                          # JPA 持久化模組
    │   ├── entity                           # 資料庫持久化實體
    │   │   ├── TemplateFieldMapping.java
    │   │   ├── TemplateSheetMapping.java
    │   │   └── ValidationPolicy.java
    │   └── repository                       # Spring Data JPA Repositories
    └── util                                 # 工具類別 (ExcelUtil, ValidationUtil 等)
```

---

## 四、 快速開始 (Quick Start)

### 環境需求
- **JDK**: Java 17 或以上版本
- **Build Tool**: Maven 3.8+ (專案內含 `mvnw` / `mvnw.cmd` wrapper)
- **Database**: MySQL 8.0+ 或 H2 Database（單元測試預設使用 H2 in-memory）

### 資料庫初始化
專案 `sql/` 目錄集中提供標準 SQL 建立與種子資料腳本：
1. [sql/init-schema.sql](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/sql/init-schema.sql)：初始化基礎資料表與範本驗證原則。
2. [sql/pr-import-init.sql](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/sql/pr-import-init.sql)：初始化請購單範本（`PR_IMPORT`）的雙層工作表與欄位對映資料。
3. [sql/po-import-init.sql](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/sql/po-import-init.sql)：初始化採購訂單範本（`PO_IMPORT`）的雙層工作表與欄位對映資料。

### 構建與執行測試
本專案所有單元測試皆採用 **BDD Gherkin 語法**，執行全套測試驗證：

```powershell
# Windows
.\mvnw.cmd clean test

# Linux / macOS
./mvnw clean test
```

> **測試結果**：涵蓋 Web 控制器、AOP 切面、雙層防腐正規化器、PR/PO 批次處理器等，共 81 個測試案例全數通過（`BUILD SUCCESS`）。

### 啟動應用程式

```powershell
# Windows
.\mvnw.cmd spring-boot:run
```

- **預設服務埠號**：`8080`
- **Swagger UI 互動式文件**：[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI 3.0 中繼資料**：[http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 五、 使用指南與程式碼範例

### 1. REST API 上傳檢核

表現層 Controller 方法只需加上 `@CustomValidate`，即可自動攔截並校驗上傳的 Excel 檔案：

```java
@RestController
@RequestMapping("/api/v1/templates")
public class UploadController {

    private final UploadCommandService uploadCommandService;
    private final UploadResourceAssembler assembler;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CustomValidate(templateName = "#resource.type") // 動態以傳入之 resource.type 作為範本代碼進行校驗
    public ResponseEntity<TemplateUploadedResource> upload(
            @Valid @ModelAttribute UploadTemplateResource resource,
            @RequestParam("file") MultipartFile file) throws IOException {

        UploadTemplateCommand command = assembler.toCommand(resource, file.getInputStream());
        uploadCommandService.uploadTemplate(command);
        return ResponseEntity.ok(assembler.toResource(resource.type(), resource.name()));
    }
}
```

### 2. 實作 MinIO / S3 串流批次處理器

若自物件儲存系統（如 MinIO、AWS S3）讀取批次檔案，只需實作 [BatchFileProcessor.java](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/iface/schedule/processor/BatchFileProcessor.java)，即可自動享有串流驗證防護：

```java
@Component
public class PrImportFileProcessor implements BatchFileProcessor {

    private final UploadCommandService uploadCommandService;

    @Override
    public String getTemplateName() {
        return "PR_IMPORT"; // 對應之範本代碼
    }

    /**
     * 原生支援 MinIO / S3 物件儲存串流處理
     * 自動由 @CustomValidate 攔截檢核，檢核通過後進入下游業務處理
     */
    @Override
    public void processStream(InputStream inputStream) throws IOException {
        log.info("開始處理自 MinIO 取得之請購匯入串流...");
        UploadTemplateCommand command = new UploadTemplateCommand(
                getTemplateName(),
                "MinIO_Stream_" + System.currentTimeMillis(),
                inputStream
        );
        uploadCommandService.uploadTemplate(command);
    }

    @Override
    public void processFile(File excelFile) throws IOException {
        try (InputStream inputStream = new FileInputStream(excelFile)) {
            processStream(inputStream);
        }
    }
}
```

**業務串接範例（呼叫端）**：
```java
// 自 MinIO 取得檔案串流
try (InputStream minioStream = minioClient.getObject(
        GetObjectArgs.builder().bucket("import-bucket").object("pr/20261029.xlsx").build())) {
    
    // 直接呼叫處理器，@CustomValidate 切面將自動執行格式與資料檢驗
    prImportFileProcessor.processStream(minioStream);
}
```

### 3. 雙層對映規則配置 (Sheet & Field Mapping)

模組提供完整的 REST API 供管理員維護工作表與欄位別名：

#### Level 1: 工作表對映 (`/api/v1/template-sheet-mappings`)
- `POST` 新增工作表對映（如將別名 `請購項目明細` 映射至標準代碼 `PR_ITEM`）。
- `GET` 查詢指定範本已啟用的工作表清單與必填設定。

#### Level 2: 欄位對映 (`/api/v1/template-field-mappings`)
- `POST` 新增欄位對映（如將別名 `單價(未稅)` 映射至內部代碼 `unitPrice`）。
- `GET /search` 動態條件搜尋欄位對映設定。

---

## 六、 測試規範與驗證成果

依據專案規範，所有測試方法皆以 **BDD Gherkin 語法（Feature, Scenario, Given-When-Then）** 進行 Javadoc 註解與情境驗證：

```java
/**
 * Feature: 批次檔案處理器之 MinIO / S3 串流匯入功能
 * Scenario: 傳入合法請購 Excel 串流時，通過 @CustomValidate 檢核並完成命令發送
 *
 * Given 具備合法請購資料之 Excel 輸入串流 (包含符合規則之 PR_HEADER 與 PR_ITEM 頁籤)
 * When 呼叫 prImportFileProcessor.processStream(inputStream)
 * Then 不拋出校驗異常，並驗證成功調用應用層服務
 */
@Test
void givenValidExcelStream_whenProcessStream_thenValidationPasses() throws Exception { ... }
```

### 測試執行指標
- **總測試案例數**：81
- **失敗 (Failures)**：0
- **錯誤 (Errors)**：0
- **跳過 (Skipped)**：0
- **涵蓋範圍**：
  - 表現層 Web API (MockMvc)
  - AOP 註解切面 (@CustomValidate + SpEL)
  - 雙層正規化防腐層 (Sheet & Header Normalizer)
  - 應用層 CRUD 服務與交易
  - 批次處理器 (File 與 InputStream 串流)

---

## 七、 相關設計規格文件

如需查閱深入的設計規格與架構決策演進，請參考 `plan/` 目錄下之詳細說明文件：

- [PO_IMPORT_IMPLEMENTATION_GUIDE.md](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/plan/PO_IMPORT_IMPLEMENTATION_GUIDE.md)：採購訂單批次匯入客製驗證實作指南與端到端測試說明。
- [PR_IMPORT_IMPLEMENTATION_GUIDE.md](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/plan/PR_IMPORT_IMPLEMENTATION_GUIDE.md)：請購單批次匯入客製驗證實作指南與端到端測試說明。
- [TEMPLATE_FIELD_MAPPING_DESIGN.md](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/plan/TEMPLATE_FIELD_MAPPING_DESIGN.md)：雙層範本對映防腐機制規格說明書 (Two-Tier Template Mapping Specification)。
- [REFACTORING_SCOPE_CHECKLIST.md](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/plan/REFACTORING_SCOPE_CHECKLIST.md)：架構重構檢核清單與變更對照矩陣。
