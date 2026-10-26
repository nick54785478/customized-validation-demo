/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

-- -----------------------------------------------------
-- Table structure & initial data for `validation_policy`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `validation_policy`;
CREATE TABLE `validation_policy` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `template_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `template_sheet_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `mapping_field_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rule` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `expression` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `priority_no` int DEFAULT NULL,
  `active_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_policy_template` (`template_name`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `validation_policy` (`id`, `type`, `template_name`, `template_sheet_name`, `mapping_field_name`, `rule`, `expression`, `error_message`, `priority_no`, `active_flag`) VALUES
(1, 'ROW', 'DEPT_PROFILE', 'MIS', 'NAME', 'ENFORCE_ROW_VALIDATION', '#checkedValue!=\'\'', '\'SheetName : \' + #sheetName + \', \'  + #excelAddress + \" 資料檢核有誤，不能為空\"', 1, 'Y'),
(2, 'ROW', 'DEPT_PROFILE', 'MIS', 'AGE', 'ENFORCE_ROW_VALIDATION', '#isNumeric(#checkedValue) && #checkedValue!=\'\' && #checkedValue.matches(\'^[1-9]\\d*$\')', '\'SheetName : \' + #sheetName + \', \'  + #excelAddress + \" 資料檢核有誤，不能為空或需為整數\"', 2, 'Y'),
(3, 'ROW', 'DEPT_PROFILE', 'MIS', 'SEX', 'ENFORCE_ROW_VALIDATION', '#checkedValue!=\'\'', '\'SheetName : \' + #sheetName + \', \'  + #excelAddress + \" 資料檢核有誤，不能為空\"', 3, 'Y'),
(4, 'ROW', 'DEPT_PROFILE', 'MIS', 'NATION_ID', 'ENFORCE_ROW_VALIDATION', '#checkedValue!=\'\' &&  #checkedValue.matches(\'^[A-Z][12]\\d{8}$\')', '\'SheetName : \' + #sheetName + \', \'  + #excelAddress + \" 資料檢核有誤，不能為空或身分證字號格式有誤 \"', 4, 'Y'),
(5, 'ROW', 'DEPT_PROFILE', 'HR', 'NAME', 'ENFORCE_ROW_VALIDATION', '#checkedValue!=\'\'', '\'SheetName : \' + #sheetName + \', \'  + #excelAddress + \" 資料檢核有誤，不能為空\"', 5, 'Y'),
(6, 'ROW', 'DEPT_PROFILE', 'HR', 'AGE', 'ENFORCE_ROW_VALIDATION', '#isNumeric(#checkedValue) && #checkedValue!=\'\' && #checkedValue.matches(\'^[1-9]\\d*$\')', '\'SheetName : \' + #sheetName + \', \'  + #excelAddress + \" 資料檢核有誤，不能為空或需為整數\"', 6, 'Y'),
(7, 'ROW', 'DEPT_PROFILE', 'HR', 'SEX', 'ENFORCE_ROW_VALIDATION', '#checkedValue!=\'\'', '\'SheetName : \' + #sheetName + \', \'  + #excelAddress + \" 資料檢核有誤，不能為空\"', 7, 'Y'),
(8, 'ROW', 'DEPT_PROFILE', 'HR', 'NATION_ID', 'ENFORCE_ROW_VALIDATION', '#checkedValue!=\'\' &&  #checkedValue.matches(\'^[A-Z][12]\\d{8}$\')', '\'SheetName : \' + #sheetName + \', \'  + #excelAddress + \" 資料檢核有誤，不能為空或身分證字號格式有誤 \"', 8, 'Y'),
(9, 'ROW', 'USER_PROFILE', 'USER', 'NAME', 'ENFORCE_ROW_VALIDATION', '#checkedValue!=\'\'', '\'SheetName : \' + #sheetName + \', \'  + #excelAddress + \" 資料檢核有誤，不能為空 \"', 9, 'Y'),
(10, 'ROW', 'USER_PROFILE', 'USER', 'AGE', 'ENFORCE_ROW_VALIDATION', '#isNumeric(#checkedValue) && #checkedValue!=\'\' && #checkedValue.matches(\'^[1-9]\\d*$\')', '\'SheetName : \' + #sheetName + \', \'  + #excelAddress + \" 資料檢核有誤，不能為空或需為整數 \"', 10, 'Y'),
(11, 'ROW', 'USER_PROFILE', 'USER', 'SEX', 'ENFORCE_ROW_VALIDATION', '#checkedValue!=\'\'', '\'SheetName : \' + #sheetName + \', \'  + #excelAddress + \" 資料檢核有誤，不能為空 \"', 11, 'Y'),
(12, 'ROW', 'USER_PROFILE', 'USER', 'NATION_ID', 'ENFORCE_ROW_VALIDATION', '#checkedValue!=\'\' &&  #checkedValue.matches(\'^[A-Z][12]\\d{8}$\')', '\'SheetName : \' + #sheetName + \', \'  + #excelAddress + \" 資料檢核有誤，不能為空或身分證字號格式有誤  \"', 12, 'Y'),
(13, 'SHEET', 'USER_PROFILE', 'USER', 'NATION_ID', '', '#validateDuplicate(#sheet, \'NATION_ID\')', '', 13, 'Y'),
(14, 'SHEET', 'USER_PROFILE', 'DEPT', 'deptList', 'VARIABLE', '{\'資訊部\',\'人力資源部\',\'會計部\',\'行銷部\'}', '', 14, 'Y'),
(15, 'SHEET', 'USER_PROFILE', 'DEPT', 'DEPT', '', '#contains(#deptList, #sheet, \'DEPT\')', '', 15, 'Y');

-- -----------------------------------------------------
-- Table structure & initial data for `template_sheet_mapping`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `template_sheet_mapping`;
CREATE TABLE `template_sheet_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主鍵識別碼',
  `template_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '範本唯一名稱 (例如: USER_PROFILE, DEPT_PROFILE)',
  `sheet_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '內部統一工作表代碼 (例如: USER, DEPT, MIS, HR)',
  `display_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Excel 實體工作表頁籤顯示文字 (例如: 使用者名單, 部門清單)',
  `sheet_index` int DEFAULT NULL COMMENT '預期工作表索引 (0-based, 可選)',
  `is_required` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '是否為必備工作表 (Y/N)',
  `active_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '啟用註記 (Y/N)',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_template_sheet` (`template_name`, `sheet_code`),
  KEY `idx_sheet_template` (`template_name`),
  KEY `idx_sheet_display` (`template_name`, `display_name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Excel 範本工作表客製對映定義表';

-- 測試資料：USER_PROFILE 範本工作表對照設定
INSERT INTO `template_sheet_mapping` (`id`, `template_name`, `sheet_code`, `display_name`, `sheet_index`, `is_required`, `active_flag`) VALUES
(1, 'USER_PROFILE', 'USER', '使用者名單', 0, 'Y', 'Y'),
(2, 'USER_PROFILE', 'DEPT', '所屬部門', 1, 'Y', 'Y');

-- 測試資料：DEPT_PROFILE 範本工作表對照設定
INSERT INTO `template_sheet_mapping` (`id`, `template_name`, `sheet_code`, `display_name`, `sheet_index`, `is_required`, `active_flag`) VALUES
(3, 'DEPT_PROFILE', 'MIS', '資訊部', 0, 'Y', 'Y'),
(4, 'DEPT_PROFILE', 'HR', '人力資源部', 1, 'Y', 'Y');

-- -----------------------------------------------------
-- Table structure & initial data for `template_field_mapping`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `template_field_mapping`;
CREATE TABLE `template_field_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主鍵識別碼',
  `template_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '範本唯一名稱 (例如: USER_PROFILE, DEPT_PROFILE)',
  `template_sheet_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工作表名稱 (例如: USER, DEPT, MIS, HR)',
  `field_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '內部統一屬性代碼 (例如: NAME, AGE, SEX, NATION_ID, DEPT)',
  `display_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Excel 實體標題文字 (例如: 姓名, 年齡, 性別, 身分證字號, 部門)',
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
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Excel 範本欄位客製對映定義表';

-- 測試資料：USER_PROFILE 範本欄位對照設定
INSERT INTO `template_field_mapping` (`id`, `template_name`, `template_sheet_name`, `field_code`, `display_name`, `column_index`, `is_required`, `data_type`, `active_flag`) VALUES
(1, 'USER_PROFILE', 'USER', 'NAME', '姓名', 1, 'Y', 'STRING', 'Y'),
(2, 'USER_PROFILE', 'USER', 'AGE', '年齡', 2, 'Y', 'NUMBER', 'Y'),
(3, 'USER_PROFILE', 'USER', 'SEX', '性別', 3, 'Y', 'STRING', 'Y'),
(4, 'USER_PROFILE', 'USER', 'NATION_ID', '身分證字號', 4, 'Y', 'STRING', 'Y'),
(5, 'USER_PROFILE', 'DEPT', 'NAME', '姓名', 1, 'N', 'STRING', 'Y'),
(6, 'USER_PROFILE', 'DEPT', 'EMP_NO', '員工編號', 2, 'N', 'STRING', 'Y'),
(7, 'USER_PROFILE', 'DEPT', 'JOB_TITLE', '職稱', 3, 'N', 'STRING', 'Y'),
(8, 'USER_PROFILE', 'DEPT', 'DEPT', '部門', 4, 'Y', 'STRING', 'Y');

-- 測試資料：DEPT_PROFILE 範本欄位對照設定
INSERT INTO `template_field_mapping` (`id`, `template_name`, `template_sheet_name`, `field_code`, `display_name`, `column_index`, `is_required`, `data_type`, `active_flag`) VALUES
(9, 'DEPT_PROFILE', 'MIS', 'NAME', '姓名', 1, 'Y', 'STRING', 'Y'),
(10, 'DEPT_PROFILE', 'MIS', 'AGE', '年齡', 2, 'Y', 'NUMBER', 'Y'),
(11, 'DEPT_PROFILE', 'MIS', 'SEX', '性別', 3, 'Y', 'STRING', 'Y'),
(12, 'DEPT_PROFILE', 'MIS', 'NATION_ID', '身分證字號', 4, 'Y', 'STRING', 'Y'),
(13, 'DEPT_PROFILE', 'HR', 'NAME', '姓名', 1, 'Y', 'STRING', 'Y'),
(14, 'DEPT_PROFILE', 'HR', 'AGE', '年齡', 2, 'Y', 'NUMBER', 'Y'),
(15, 'DEPT_PROFILE', 'HR', 'SEX', '性別', 3, 'Y', 'STRING', 'Y'),
(16, 'DEPT_PROFILE', 'HR', 'NATION_ID', '身分證字號', 4, 'Y', 'STRING', 'Y');

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;