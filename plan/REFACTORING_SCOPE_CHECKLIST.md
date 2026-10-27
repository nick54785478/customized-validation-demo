# 客製化驗證模組 (customized-validation-module) 架構重構與檢核清單

本文件記錄本模組依據 [AGENTS.md](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/.agents/AGENTS.md) 規範與模組定位進行架構重構的整體 Scope、執行計畫與逐項檢核清單，供後續實作與驗收審查使用。

---

## 一、 重構背景與核心決策

### 1. 模組定位 vs. 業務微服務
- **專案定位**：本專案為 `customized-validation-module`，本質上是一個**通用的驗證組件/模組 (Library / Starter / Component)**，而非承載特定業務流程（如採購單、工單）的獨立微服務。
- **Domain 退階至 Infrastructure 之核心理由**：
  1. 在業務微服務中，DDD Domain Layer 封裝的是業務核心聚合根（如 Order、Task），嚴禁依賴 Spring、JPA 與資料庫。
  2. 但在通用驗證模組中，所謂的「驗證規則（ValidationPolicy）」並非業務領域的聚合根，而是驗證引擎用來讀取、設定檢核規則的持久化資料模型（Persistence Model）。
  3. 目前專案中錯誤地建立了 `application.domain.policy.aggregate.ValidationPolicy`，並在其中引入 JPA 實體註解（`@Entity`, `@Table`）與 Lombok，既破壞了 DDD 純領域的規範，也不符合模組的真實職責。
  4. 因此，**將 Domain 層的內容退階至 Infrastructure (infra)**，回歸為基礎設施層的持久化實體（JPA Entity），徹底消除不適當的 `application.domain` 層級，是完全精確且合理的架構收斂。

### 2. 命名慣例同步
- `infra` = `infrastructure` (基礎設施層)
- `iface` = `presentation` (表現層/介面層)

---

## 二、 目錄結構變更對照 (Before vs. After)

```diff
 com.example.demo
-├── application
-│   └── domain (【刪除】：模組無業務 Aggregate，退階至 infra)
-│       └── policy
-│           ├── aggregate
-│           │   └── ValidationPolicy.java  --> 移至 infra.persistence.entity
-│           └── command
-│               └── ValidateTemplateCommand.java --> (【清理】：目前未被使用)
 ├── application
+│   ├── exception
+│   │   ├── ExcelValidationException.java (應用層校驗例外)
+│   │   └── ValidationException.java (應用層業務例外)
-│   ├── port
-│   │   └── ExcelValidatorPort.java (依賴 MultipartFile 技術物件)
+│   ├── port
+│   │   ├── in
+│   │   │   └── ValidateExcelUseCase.java (解耦技術物件，使用 InputStream / 純資料)
+│   │   └── out
+│   │       └── ExcelValidatorPort.java (Outbound Port 合約)
 │   └── service
 │       └── UploadCommandService.java
+│       └── ValidateExcelApplicationService.java (實作 ValidateExcelUseCase)
+│   └── shared (【收斂】：由根目錄 share 遷入 Application 共享套件)
+│       ├── command
+│       │   └── UploadTemplateCommand.java (【業務指令】：以 Java 17 record 實作之不可變命令載體，零技術侵入)
+│       └── vo
+│           ├── ValidationError.java (【值物件】：以 Java 17 record 實作之不可變檢核錯誤快照，廢除原籠統 model)
+│           └── YesNo.java (【值物件】：純 Java 列舉值物件，零 Lombok / 框架技術侵入)
 ├── iface (presentation)
+│   ├── advice
+│   │   └── GlobalExceptionHandler.java (@RestControllerAdvice 例外轉譯)
 │   ├── aspect
 │   │   └── CustomValidationAspect.java
-│   ├── dto
-│   │   ├── TemplateUploadedResource.java
-│   │   └── UploadTemplateResource.java
+│   ├── resource
+│   │   ├── in
+│   │   │   └── UploadTemplateResource.java (V + N + Resource)
+│   │   └── out
+│   │       ├── BaseExceptionResponse.java
+│   │       ├── BaseExceptionsResponse.java
+│   │       └── TemplateUploadedResource.java (N + Ved + Resource)
+│   ├── assembler
+│   │   └── UploadResourceAssembler.java (純資料轉換防腐層)
 │   └── rest
 │       └── UploadController.java
 ├── infra (infrastructure)
 │   ├── adapter
 │   │   └── ExcelValidatorAdapter.java
 │   ├── cv (客製驗證核心引擎)
+│   │   ├── normalizer
+│   │   │   └── TemplateHeaderNormalizer.java (【標題防腐層】：標題前置校驗與欄位鍵值正規化)
 │   │   ├── parser
 │   │   ├── shared
 │   │   └── validator
-│   └── repository
-│       └── ValidationPolicyRepository.java
+│   └── persistence
+│       ├── entity
+│       │   ├── TemplateFieldMapping.java (【欄位對映實體】：客製標題與內部代碼對照)
+│       │   └── ValidationPolicy.java (退階至此，明確為 JPA 實體)
+│       └── repository
+│           ├── TemplateFieldMappingRepository.java (Spring Data JPA)
+│           └── ValidationPolicyRepository.java (Spring Data JPA)
+│   └── util (【收斂】：由全域 util 遷入 Infrastructure)
+│       ├── BaseDataTransformer.java
+│       ├── ExcelUtil.java
+│       ├── ValidationUtil.java
+│       └── VariableUtil.java
-├── share (【刪除】：指令與業務列舉已更名收斂至 application.shared)
-├── util (【刪除】：工具類皆屬於基礎設施技術操作，已收斂至 infra.util)
```

---

## 三、 分階段實施與檢核清單 (Checklist)

### 【Phase 1】Domain 內容退階至 Infrastructure (核心優先)
> **目標**：移除 `application.domain` 層級，將 `ValidationPolicy` 退階為 `infra.persistence` 下的資料實體，修正所有依賴引用。

- [x] **1.1 遷移 JPA Entity 至 infra**
  - 將 `com.example.demo.application.domain.policy.aggregate.ValidationPolicy` 搬移至 `com.example.demo.infra.persistence.entity.ValidationPolicy`。
  - 保留必要的 JPA 標註 (`@Entity`, `@Table`, `@Id` 等) 與清晰的 Javadoc。
- [x] **1.2 遷移 Repository 至 infra.persistence**
  - 將 `com.example.demo.infra.repository.ValidationPolicyRepository` 搬移至 `com.example.demo.infra.persistence.repository.ValidationPolicyRepository`。
  - 調整泛型引用指向新的 `ValidationPolicy`。
- [x] **1.3 清理未使用的 Command**
  - 評估並移除未使用的 `com.example.demo.application.domain.policy.command.ValidateTemplateCommand`。
- [x] **1.4 刪除空的 domain 目錄**
  - 徹底移除 `com.example.demo.application.domain` 套件。
- [x] **1.5 修正呼叫端的 import 引用**
  - [x] [CustomValidator.java](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/infra/cv/validator/CustomValidator.java)
  - [x] [ExcelValidatorAdapter.java](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/main/java/com/example/demo/infra/adapter/ExcelValidatorAdapter.java)
  - [x] [ExcelValidateServiceTest.java](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/src/test/java/com/example/demo/service/ExcelValidateServiceTest.java)
- [x] **1.6 階段性編譯與測試驗證**
  - 執行 `.\mvnw.cmd test-compile` 與 `.\mvnw.cmd test`，確認無編譯錯誤且既有測試通過 (BUILD SUCCESS)。

---

### 【Phase 2】Port / Adapter 技術物件解耦與規範收斂
> **目標**：依照六角形架構規範，解除 Port 介面對 Spring Web `MultipartFile` 的技術依賴，並規範 Port 目錄結構。

- [x] **2.1 解耦技術類別依賴**
  - 依據 AGENTS.md「Port 方法嚴禁接發技術類物件」，將原本接收 `MultipartFile` 的簽名改為標準 Java 串流（如 `InputStream`）或純位元組/資料載體。
- [x] **2.2 規範 Port 套件結構與命名**
  - 建立 `application.port.in` 與 `application.port.out` 分層。
  - 若作為應用層開放的驗證合約，將介面命名為 `...UseCase`（如 `ValidateExcelUseCase`）並放置於 `port/in/`；
  - 若作為外發適配器的合約，保留以 `...Port` 結尾並放置於 `port/out/`。
- [x] **2.3 調整 Adapter 與調用端實作**
  - 調整 `ExcelValidatorAdapter` 實作新簽名與介面。
  - 調整 `CustomValidationAspect` 或調用端，在表現層/切面層將 `MultipartFile` 提取為串流傳入 Port。

---

### 【Phase 3】iface (Presentation) 規範化與防腐層
> **目標**：符合 AGENTS.md 對表現層 Resource 命名、分類及 Assembler 防腐轉換的規範。

- [x] **3.1 Resource 套件拆分**
  - 建立 `iface.resource.in` 與 `iface.resource.out`。
  - 搬移 `UploadTemplateResource` 至 `iface.resource.in` (符合 `V + N + Resource`)。
  - 搬移 `TemplateUploadedResource` 至 `iface.resource.out` (符合 `N + Ved + Resource`)。
- [x] **3.2 建立 Assembler 負責資料轉換**
  - 建立 `iface.assembler.UploadResourceAssembler`。
  - 取代 Controller 內直接調用 `BaseDataTransformer` 的作法，維持 Resource 作為純資料載體（Pure Data Carrier）之原則。
- [x] **3.3 調整 UploadController**
  - 注入 `UploadResourceAssembler` 進行請求轉換，並調整相關 Resource import。

---

### 【Phase 4】測試與 Javadoc 規範落實
> **目標**：落實 AGENTS.md 第 4、5 點要求之 Javadoc 與 Gherkin 測試規範。

- [ ] **4.1 補齊類別與介面之 Javadoc**
  - 為 `ValidationPolicy`、`ValidationPolicyRepository`、Port/UseCase 及 Adapter 補齊極致清晰的 Javadoc，說明架構職責與業務語意。
- [ ] **4.2 撰寫 Gherkin 語法測試註解**
  - 在 `ExcelValidateServiceTest` 及所有測試方法上方，加入以 Gherkin 語法（`Feature`, `Scenario`, `Given`, `When`, `Then`）撰寫之 Javadoc。
- [ ] **4.3 擴充 UseCase 測試案例**
  - 針對正常檢核、空值規則、格式錯誤、例外拋出（`ExcelValidationException`）等分支撰寫單元/整合測試。
- [ ] **4.4 最終全域測試驗證**
  - 執行 `.\mvnw.cmd clean test`，確認輸出 `BUILD SUCCESS`。

---

## 四、 受影響檔案清單矩陣 (File Change Matrix)

| 檔案路徑 | 變更類型 | 所屬 Phase | 說明 |
| :--- | :---: | :---: | :--- |
| `src/main/java/com/example/demo/application/domain/policy/aggregate/ValidationPolicy.java` | **Move/Rename** | Phase 1 | 遷移至 `infra.persistence.entity` |
| `src/main/java/com/example/demo/application/domain/policy/command/ValidateTemplateCommand.java` | **Delete** | Phase 1 | 未使用，予以移除 |
| `src/main/java/com/example/demo/infra/repository/ValidationPolicyRepository.java` | **Move** | Phase 1 | 遷移至 `infra.persistence.repository` |
| `src/main/java/com/example/demo/infra/cv/validator/CustomValidator.java` | **Modify** | Phase 1 | 更新 `ValidationPolicy` import |
| `src/main/java/com/example/demo/infra/adapter/ExcelValidatorAdapter.java` | **Modify** | Phase 1, 2 | 更新 import，並配合 Phase 2 解耦 `MultipartFile` |
| `src/main/java/com/example/demo/application/port/ExcelValidatorPort.java` | **Modify/Move** | Phase 2 | 依 UseCase/Port 規範重整，並解耦 `MultipartFile` |
| `src/main/java/com/example/demo/iface/aspect/CustomValidationAspect.java` | **Modify** | Phase 2 | 調整 Port/UseCase 調用與傳參轉換 |
| `src/main/java/com/example/demo/iface/dto/UploadTemplateResource.java` | **Move** | Phase 3 | 遷移至 `iface.resource.in` |
| `src/main/java/com/example/demo/iface/dto/TemplateUploadedResource.java` | **Move** | Phase 3 | 遷移至 `iface.resource.out` |
| `src/main/java/com/example/demo/iface/assembler/UploadResourceAssembler.java` | **Create** | Phase 3 | 新增 Assembler 負責轉換 |
| `src/main/java/com/example/demo/iface/rest/UploadController.java` | **Modify** | Phase 3 | 引用 Assembler 與新 Resource 路徑 |
| `src/main/java/com/example/demo/application/exception/*` | **Create/Move** | Refactoring | 建立應用層例外 `ValidationException`, `ExcelValidationException` |
| `src/main/java/com/example/demo/iface/advice/GlobalExceptionHandler.java` | **Create** | Refactoring | 建立表現層全域例外轉譯器，輸出 `BaseExceptionResponse` |
| `src/main/java/com/example/demo/infra/util/*` | **Move** | Refactoring | 將全域 `util` 遷入 `infra.util`，解決工具類分層問題 |
| `src/main/java/com/example/demo/application/shared/*` | **Move/Rename** | Refactoring | 將全域 `share` 更名為 `shared` 並遷入 `application.shared` |
| `src/main/java/com/example/demo/application/shared/vo/ValidationError.java` | **Create/Refactor** | Refactoring | 以 Java 17 record 實作值物件，取代並刪除 `application.model` |
| `src/main/java/com/example/demo/infra/persistence/entity/TemplateSheetMapping.java` | **Create** | Mapping | 建立範本工作表對映實體，支援客製頁籤名稱 (Level 1 ACL) |
| `src/main/java/com/example/demo/infra/persistence/repository/TemplateSheetMappingRepository.java` | **Create** | Mapping | 建立範本工作表對映持久化倉儲 |
| `src/main/java/com/example/demo/infra/cv/normalizer/TemplateSheetNormalizer.java` | **Create** | Mapping | 建立工作表防腐層，支援前置工作表防呆校驗與頁籤代碼正規化 |
| `src/main/java/com/example/demo/infra/persistence/entity/TemplateFieldMapping.java` | **Create** | Mapping | 建立範本欄位對映實體，解耦實體標題與內部屬性 (Level 2 ACL) |
| `src/main/java/com/example/demo/infra/persistence/repository/TemplateFieldMappingRepository.java` | **Create** | Mapping | 建立範本欄位對映持久化倉儲 |
| `src/main/java/com/example/demo/infra/cv/normalizer/TemplateHeaderNormalizer.java` | **Create** | Mapping | 建立標題防腐層，支援前置標題防呆校驗與鍵值正規化 |
| `src/main/java/com/example/demo/infra/adapter/ExcelValidatorAdapter.java` | **Modify** | Mapping | 整合工作表與標題雙層對映倉儲與正規化防腐器 |
| `src/test/java/com/example/demo/infra/cv/normalizer/TemplateSheetNormalizerTest.java` | **Create** | Testing | 建立工作表正規化與防呆單元測試 (含 Gherkin Javadoc) |
| `src/main/java/com/example/demo/application/shared/command/*MappingCommand.java` | **Create** | Mapping CRUD | 建立/更新/刪除工作表與欄位對映業務指令 (Java 17 record) |
| `src/main/java/com/example/demo/application/shared/query/*MappingQuery.java` | **Create** | Mapping CRUD | 單筆與條件搜尋工作表與欄位對映查詢物件 (Java 17 record) |
| `src/main/java/com/example/demo/application/dto/*Mapping*Result.java` | **Create** | Mapping CRUD | 查詢與搜尋工作表與欄位對映純資料載體 (DTO record) |
| `src/main/java/com/example/demo/application/port/in/*MappingUseCase.java` | **Create** | Mapping CRUD | 工作表與欄位對映 CRUD 與 Search 之 Inbound Port 介面 |
| `src/main/java/com/example/demo/application/port/out/*MappingRepositoryPort.java` | **Create** | Mapping CRUD | 工作表與欄位對映持久化 Outbound Port 介面 (零技術物件) |
| `src/main/java/com/example/demo/application/service/*MappingApplicationService.java` | **Create** | Mapping CRUD | 實作 Inbound Port 之應用層服務 (交易控制與業務校驗) |
| `src/main/java/com/example/demo/infra/assembler/*MappingDtoAssembler.java` | **Create** | Mapping CRUD | 基礎設施層持久化實體與應用層 DTO 轉換組裝器 |
| `src/main/java/com/example/demo/infra/adapter/*MappingRepositoryAdapter.java` | **Create** | Mapping CRUD | 實作 Outbound Port 之持久化適配器 (含 JPA Specification 動態搜尋) |
| `src/main/java/com/example/demo/iface/resource/in/*MappingResource.java` | **Create** | Mapping CRUD | 表現層輸入 Request 資料載體 (純資料物件) |
| `src/main/java/com/example/demo/iface/resource/out/*Mapping*Resource.java` | **Create** | Mapping CRUD | 表現層輸出 Response 資料載體 (純資料物件) |
| `src/main/java/com/example/demo/iface/assembler/*MappingResourceAssembler.java` | **Create** | Mapping CRUD | 表現層 Resource 與應用層 Command/Query/Result 防腐組裝器 |
| `src/main/java/com/example/demo/iface/rest/*MappingController.java` | **Create** | Mapping CRUD | 工作表與欄位對映 REST API 控制器 (POST, PUT, DELETE, GET, Search) |
| `src/test/java/com/example/demo/application/service/*MappingApplicationServiceTest.java` | **Create** | Testing | 應用服務單元測試 (含 BDD Gherkin Javadoc 規範，共 18 個案例) |
| `src/test/java/com/example/demo/iface/rest/*MappingControllerTest.java` | **Create** | Testing | 表現層 WebMvc MockMvc 整合測試 (共 12 個案例) |
| `plan/TEMPLATE_FIELD_MAPPING_DESIGN.md` | **Create/Update** | Mapping | 雙層範本對映防腐機制規格說明書 (Two-Tier Template Mapping Specification) |
| `plan/PR_IMPORT_IMPLEMENTATION_GUIDE.md` | **Create/Update** | Verification | PR Import 請購單匯入客製驗證實作指南與端到端測試說明 |
| `src/main/java/com/example/demo/config/OpenApiConfig.java` | **Create** | OpenAPI | SpringDoc OpenAPI 3.0 / Swagger UI 全域中繼規格配置 |
| `src/main/java/com/example/demo/config/WebConfig.java` | **Create** | OpenAPI | 表現層 Web 轉換器配置，相容 Swagger UI Multipart 傳輸 |
| `src/main/java/com/example/demo/iface/resource/**/*Resource.java` | **Modify** | OpenAPI | 表現層 18 個 Request/Response 資源全面補齊 `@Schema` 註解 |
| `src/test/java/com/example/demo/service/ExcelValidateServiceTest.java` | **Modify** | Phase 1, 4 | 更新 import、採用 `ValidationError` 並補齊 Gherkin Javadoc |
| `src/test/java/com/example/demo/service/PrImportValidateTest.java` | **Create** | Testing | PR Import 請購單批次匯入檢核單元/整合測試套件 (5 個 Gherkin 案例) |

---

## 五、 驗收標準 (Acceptance Criteria)

1. **結構規範**：
   - 專案內不存在任何 `com.example.demo.application.domain` 套件。
   - `ValidationPolicy` 實體與 Repository 正確內聚於 `infra.persistence` 之中。
2. **零技術侵入**：
   - Application 層的 Port / UseCase 介面中不存在任何 `MultipartFile` 或 Spring Web 物件依賴。
3. **編譯與測試**：
   - 執行 `.\mvnw.cmd clean test` 無任何編譯錯誤，所有單元與整合測試全數通過（`BUILD SUCCESS`）。
4. **文件完整性**：
   - 每個實作階段完成後，本文件之 Checklist 應同步勾選狀態。
