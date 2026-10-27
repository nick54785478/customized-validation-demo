-- ==============================================================================
-- PR Import (請購單批次匯入) 客製化驗證與雙層防腐層資料庫設定腳本
-- 檔案名稱: pr-import-init.sql
-- 說明:
--   1. 提供 PR_IMPORT (請購單批次匯入) 範本之資料表結構 (DDL) 與初始化資料 (DML)
--   2. Level 1: 工作表層級防腐對映 (template_sheet_mapping)
--   3. Level 2: 欄位標題層級防腐對映 (template_field_mapping)
--   4. 客製驗證規則與 SpEL 表達式 (validation_policy)
--   5. 具備冪等性 (Idempotent)，可重複執行
-- ==============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------------------------
-- 1. 建立資料表結構 (DDL)
-- ------------------------------------------------------------------------------

-- 1.1 工作表層級客製對映定義表 (Level 1 ACL)
CREATE TABLE IF NOT EXISTS `template_sheet_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主鍵識別碼',
  `template_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '範本唯一名稱 (例如: PR_IMPORT, USER_PROFILE)',
  `sheet_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '內部統一工作表代碼 (例如: PR_ITEM, USER, DEPT)',
  `display_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Excel 實體工作表頁籤顯示文字 (例如: 請購明細清單, 使用者名單)',
  `sheet_index` int DEFAULT NULL COMMENT '預期工作表索引 (0-based, 可選)',
  `is_required` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '是否為必備工作表 (Y/N)',
  `active_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '啟用註記 (Y/N)',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_template_sheet` (`template_name`, `sheet_code`),
  KEY `idx_sheet_template` (`template_name`),
  KEY `idx_sheet_display` (`template_name`, `display_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Excel 範本工作表客製對映定義表 (Level 1 ACL)';

-- 1.2 欄位標題層級客製對映定義表 (Level 2 ACL)
CREATE TABLE IF NOT EXISTS `template_field_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主鍵識別碼',
  `template_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '範本唯一名稱 (例如: PR_IMPORT, USER_PROFILE)',
  `template_sheet_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工作表內部代碼 (例如: PR_ITEM, USER)',
  `field_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '內部統一屬性代碼 (CamelCase，例如: itemNo, quantity, unitPrice)',
  `display_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Excel 實體標題文字 (例如: 物料料號, 需求數量, 預算單價)',
  `column_index` int DEFAULT NULL COMMENT '實體欄位預期順序 (1-based, 可選)',
  `is_required` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '是否為必填/必備欄位 (Y/N)',
  `data_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'STRING' COMMENT '資料型別 (STRING, NUMBER, DATE...)',
  `active_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '啟用註記 (Y/N)',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_template_sheet_field` (`template_name`, `template_sheet_name`, `field_code`),
  KEY `idx_template_sheet` (`template_name`, `template_sheet_name`),
  KEY `idx_template_sheet_display` (`template_name`, `template_sheet_name`, `display_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Excel 範本欄位客製對映定義表 (Level 2 ACL)';

-- 1.3 範本檢核原則與 SpEL 規則策略表
CREATE TABLE IF NOT EXISTS `validation_policy` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主鍵流水號',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '驗證類型 (ROW: 逐列檢核, SHEET: 整表/變數檢核)',
  `template_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '範本唯一名稱 (例如: PR_IMPORT)',
  `template_sheet_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目標工作表內部代碼 (例如: PR_ITEM)',
  `mapping_field_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '對應之映射欄位代碼 (例如: itemNo, validCostCenters)',
  `rule` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '規則代碼 (ENFORCE_ROW_VALIDATION, VARIABLE, VALIDATE_DUPLICATE)',
  `expression` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'SpEL 表達式字串',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '錯誤訊息模板 (支援 SpEL 動態解析)',
  `priority_no` int DEFAULT NULL COMMENT '規則執行優先順序',
  `active_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否啟用 (Y/N)',
  PRIMARY KEY (`id`),
  KEY `idx_policy_template` (`template_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='範本客製驗證規則原則表';

-- ------------------------------------------------------------------------------
-- 2. 清理舊資料 (確保重複執行時的冪等性)
-- ------------------------------------------------------------------------------
DELETE FROM `template_sheet_mapping` WHERE `template_name` = 'PR_IMPORT';
DELETE FROM `template_field_mapping` WHERE `template_name` = 'PR_IMPORT';
DELETE FROM `validation_policy`       WHERE `template_name` = 'PR_IMPORT';

-- ------------------------------------------------------------------------------
-- 3. 寫入 Level 1: 工作表層級防腐對映 (template_sheet_mapping)
--    說明: 支援業務人員將頁籤命名為中文「請購明細清單」，防腐層將其正規化為「PR_ITEM」
-- ------------------------------------------------------------------------------
INSERT INTO `template_sheet_mapping` 
(`template_name`, `sheet_code`, `display_name`, `sheet_index`, `is_required`, `active_flag`) 
VALUES 
('PR_IMPORT', 'PR_ITEM', '請購明細清單', 0, 'Y', 'Y');

-- ------------------------------------------------------------------------------
-- 4. 寫入 Level 2: 欄位標題層級防腐對映 (template_field_mapping)
--    說明: 業務上傳之中文首列標題將自動正規化為內部 CamelCase 屬性代碼，並執行前置防呆
-- ------------------------------------------------------------------------------
INSERT INTO `template_field_mapping` 
(`template_name`, `template_sheet_name`, `field_code`, `display_name`, `column_index`, `is_required`, `data_type`, `active_flag`) 
VALUES 
('PR_IMPORT', 'PR_ITEM', 'itemNo',       '物料料號', 1, 'Y', 'STRING', 'Y'),
('PR_IMPORT', 'PR_ITEM', 'itemName',     '物料品名', 2, 'Y', 'STRING', 'Y'),
('PR_IMPORT', 'PR_ITEM', 'spec',         '規格型號', 3, 'N', 'STRING', 'Y'),
('PR_IMPORT', 'PR_ITEM', 'quantity',     '需求數量', 4, 'Y', 'NUMBER', 'Y'),
('PR_IMPORT', 'PR_ITEM', 'unitPrice',    '預算單價', 5, 'Y', 'NUMBER', 'Y'),
('PR_IMPORT', 'PR_ITEM', 'costCenter',   '成本中心', 6, 'Y', 'STRING', 'Y'),
('PR_IMPORT', 'PR_ITEM', 'requiredDate', '需求交期', 7, 'Y', 'DATE',   'Y');

-- ------------------------------------------------------------------------------
-- 5. 寫入客製驗證規則與 SpEL 策略 (validation_policy)
-- ------------------------------------------------------------------------------
INSERT INTO `validation_policy` 
(`type`, `template_name`, `template_sheet_name`, `mapping_field_name`, `rule`, `expression`, `error_message`, `priority_no`, `active_flag`) 
VALUES 
-- 5.1 [SHEET 級別] 注入合法成本中心清單變數 (VARIABLE)
('SHEET', 'PR_IMPORT', 'PR_ITEM', 'validCostCenters', 'VARIABLE', 
 '{''CC001'', ''CC002'', ''CC003'', ''IT001'', ''HR001''}', 
 '', 1, 'Y'),

-- 5.2 [SHEET 級別] 整表物料料號唯一性檢查 (同份請購清單料號不可重複)
('SHEET', 'PR_IMPORT', 'PR_ITEM', 'itemNo', 'VALIDATE_DUPLICATE', 
 '#validateDuplicate(#sheet, ''itemNo'')', 
 '', 2, 'Y'),

-- 5.3 [ROW 級別] 物料料號為必填項
('ROW', 'PR_IMPORT', 'PR_ITEM', 'itemNo', 'ENFORCE_ROW_VALIDATION', 
 '#checkedValue != null && #checkedValue.trim() != ''''', 
 '''SheetName = '' + #sheetName + '', '' + #excelAddress + '' [物料料號] 為必填項目，不可為空''', 3, 'Y'),

-- 5.4 [ROW 級別] 物料品名為必填項
('ROW', 'PR_IMPORT', 'PR_ITEM', 'itemName', 'ENFORCE_ROW_VALIDATION', 
 '#checkedValue != null && #checkedValue.trim() != ''''', 
 '''SheetName = '' + #sheetName + '', '' + #excelAddress + '' [物料品名] 為必填項目，不可為空''', 4, 'Y'),

-- 5.5 [ROW 級別] 需求數量必須為大於 0 的正整數
('ROW', 'PR_IMPORT', 'PR_ITEM', 'quantity', 'ENFORCE_ROW_VALIDATION', 
 '#isNumeric(#checkedValue) && T(java.lang.Integer).parseInt(#checkedValue) > 0', 
 '''SheetName = '' + #sheetName + '', '' + #excelAddress + '' [需求數量] 必須為大於 0 的正整數，當前值為 ['' + #checkedValue + '']''', 5, 'Y'),

-- 5.6 [ROW 級別] 預算單價必須為大於 0 的有效數值
('ROW', 'PR_IMPORT', 'PR_ITEM', 'unitPrice', 'ENFORCE_ROW_VALIDATION', 
 '#isNumeric(#checkedValue) && T(java.lang.Double).parseDouble(#checkedValue) > 0', 
 '''SheetName = '' + #sheetName + '', '' + #excelAddress + '' [預算單價] 必須為大於 0 的有效數值，當前值為 ['' + #checkedValue + '']''', 6, 'Y'),

-- 5.7 [ROW 級別] 成本中心代碼必須存在於核准清單內
('ROW', 'PR_IMPORT', 'PR_ITEM', 'costCenter', 'ENFORCE_ROW_VALIDATION', 
 '#validCostCenters.contains(#checkedValue)', 
 '''SheetName = '' + #sheetName + '', '' + #excelAddress + '' [成本中心] 代碼 ['' + #checkedValue + ''] 不存在或非有效清單''', 7, 'Y'),

-- 5.8 [ROW 級別] 需求交期必須符合 YYYY-MM-DD 格式
('ROW', 'PR_IMPORT', 'PR_ITEM', 'requiredDate', 'ENFORCE_ROW_VALIDATION', 
 '#checkedValue != null && #checkedValue.matches(''^\\d{4}-\\d{2}-\\d{2}$'')', 
 '''SheetName = '' + #sheetName + '', '' + #excelAddress + '' [需求交期] 格式不合規，需為 YYYY-MM-DD (例如 2026-12-31)''', 8, 'Y');

SET FOREIGN_KEY_CHECKS = 1;
