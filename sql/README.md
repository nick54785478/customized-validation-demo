# 資料庫初始化與範本設定腳本 (Database Initialization Scripts)

本目錄集中存放 `customized-validation-module` 的資料庫初始化 (DDL) 與各範本之種子資料 (DML) 腳本。

---

## 📂 腳本清單與說明

| 腳本名稱 | 適用範本 | 說明 |
| :--- | :--- | :--- |
| [init-schema.sql](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/sql/init-schema.sql) | 全域 / 基礎 | 建立核心資料表結構 (`validation_policy`, `template_sheet_mapping`, `template_field_mapping`)，並注入系統基礎示範範本 (`USER_PROFILE`, `DEPT_PROFILE`) 之檢核規則與對映。 |
| [pr-import-init.sql](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/sql/pr-import-init.sql) | `PR_IMPORT` | **請購單批次匯入**範本專屬設定：包含 Level 1 工作表防腐對映 (`PR_ITEM` $\leftrightarrow$ `請購明細清單`)、Level 2 欄位標題對映 (7 個欄位) 與 SpEL 商業檢核原則（具備冪等性，可重複執行）。 |
| [po-import-init.sql](file:///d:/桌面/ModuleWorkSpace/customized-validation-module/sql/po-import-init.sql) | `PO_IMPORT` | **採購訂單批次匯入**範本專屬設定：包含 Level 1 工作表防腐對映 (`PO_ITEM` $\leftrightarrow$ `採購訂單明細`)、Level 2 欄位標題對映 (11 個欄位)、核准供應商/幣別/單位清單變數注入與 SpEL 商業檢核原則（具備冪等性，可重複執行）。 |

---

## 🚀 建議執行順序 (Execution Guide)

當部署至 MySQL 資料庫時，請依序執行下列腳本：

```bash
# 1. 建立資料庫 (若尚未建立)
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS customize_validation DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 執行基礎結構與預設範本
mysql -u root -p customize_validation < sql/init-schema.sql

# 3. 匯入 PR 請購單範本設定 (可選)
mysql -u root -p customize_validation < sql/pr-import-init.sql

# 4. 匯入 PO 採購訂單範本設定 (可選)
mysql -u root -p customize_validation < sql/po-import-init.sql
```

> **注意**：
> 本專案單元與整合測試預設使用 H2 In-Memory Database，實體測試會動態建立模擬上下文，無需預先於本機安裝 MySQL。
