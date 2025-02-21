DROP TABLE IF EXISTS `validation_policy`;
CREATE TABLE `validation_policy` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `template_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `template_sheet_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `mapping_field_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rule` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `expression` text COLLATE utf8mb4_unicode_ci,
  `error_message` text COLLATE utf8mb4_unicode_ci,
  `priority_no` int DEFAULT NULL,
  `active_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `validation_policy` (`id`, `type`, `template_name`, `template_sheet_name`, `mapping_field_name`, `rule`, `expression`, `error_message`, `priority_no`, `active_flag`) VALUES
(1, 'ROW', 'USER_PROFILE', 'USER_PROFILE', 'name', 'ENFORCE_ROW_VALIDATION', '#checkedValue!=\'\'', '#excelAddress + \" 資料檢核有誤，不能為空\"', 1, 'Y');
INSERT INTO `validation_policy` (`id`, `type`, `template_name`, `template_sheet_name`, `mapping_field_name`, `rule`, `expression`, `error_message`, `priority_no`, `active_flag`) VALUES
(2, 'ROW', 'USER_PROFILE', 'USER_PROFILE', 'age', 'ENFORCE_ROW_VALIDATION', '#checkedValue!=\'\' && #checkedValue.matches(\'^[1-9]\\d*$\')', '#excelAddress + \" 資料檢核有誤，不能為空或需為整數\"', 2, 'Y');
INSERT INTO `validation_policy` (`id`, `type`, `template_name`, `template_sheet_name`, `mapping_field_name`, `rule`, `expression`, `error_message`, `priority_no`, `active_flag`) VALUES
(3, 'ROW', 'USER_PROFILE', 'USER_PROFILE', 'sex', 'ENFORCE_ROW_VALIDATION', '#checkedValue!=\'\'', '#excelAddress + \" 資料檢核有誤，不能為空\"', 3, 'Y');
INSERT INTO `validation_policy` (`id`, `type`, `template_name`, `template_sheet_name`, `mapping_field_name`, `rule`, `expression`, `error_message`, `priority_no`, `active_flag`) VALUES
(4, 'ROW', 'USER_PROFILE', 'USER_PROFILE', 'nationId', 'ENFORCE_ROW_VALIDATION', '#checkedValue!=\'\' &&  #checkedValue.matches(\'^[A-Z][12]\\d{8}$\')', '#excelAddress + \" 資料檢核有誤，不能為空或身分證字號格式有誤 \"', 4, 'Y'),
(5, 'SHEET', 'USER_PROFILE', 'USER_PROFILE', '', 'VARIABLE', '', '', 5, 'Y');

