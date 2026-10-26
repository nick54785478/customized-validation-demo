package com.example.demo.infra.persistence.entity;

import com.example.demo.application.shared.vo.YesNo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 範本客製驗證規則實體 (Validation Policy Entity)
 * <p>
 * 在客製化驗證模組 (customized-validation-module) 中，本類別退階至基礎設施持久化層 (Infrastructure Persistence Layer)，
 * 作為資料庫 {@code validation_policy} 資料表的 ORM 實體對象，負責儲存與讀取範本驗證原則、SpEL 表達式與錯誤訊息模板。
 * </p>
 */
@Entity
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "validation_policy")
public class ValidationPolicy {

	/**
	 * 主鍵流水號
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 驗證類型，例如 ROW (單列逐筆檢核) 或 SHEET (整張工作表檢核)
	 */
	private String type;

	/**
	 * 範本名稱，用以群組化不同 Excel 範本的檢核規則
	 */
	@Column(name = "template_name")
	private String templateName;

	/**
	 * 範本內的目標工作表名稱 (Sheet Name)
	 */
	@Column(name = "template_sheet_name")
	private String templateSheetName;

	/**
	 * 對應的欄位名稱 (Mapping Field Name)
	 */
	@Column(name = "mapping_field_name")
	private String mappingFieldName;

	/**
	 * 檢核規則代碼，例如: ENFORCE_ROW_VALIDATION (強制逐列驗證，即時欄位為空仍須驗證)
	 */
	private String rule;

	/**
	 * SpEL 表達式字串，由客製驗證引擎評估執行
	 */
	private String expression;

	/**
	 * 檢核失敗時呈現的錯誤訊息範本 (支援 SpEL 表達式解析動態參數)
	 */
	@Column(name = "error_message")
	private String errorMessage;

	/**
	 * 規則執行優先順序
	 */
	@Column(name = "priority_no")
	private Integer priorityNo;

	/**
	 * 是否啟用 (Y/N)
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "active_flag")
	private YesNo activeFlag;

	/**
	 * 測試與自訂建構子
	 *
	 * @param type              驗證類型 (ROW, SHEET)
	 * @param templateSheetName 工作表名稱
	 * @param rule              驗證規則代碼
	 * @param mappingFieldName  映射欄位名稱
	 * @param expression        SpEL 表達式
	 * @param errorMessage      錯誤訊息範本
	 */
	public ValidationPolicy(String type, String templateSheetName, String rule, String mappingFieldName, String expression, String errorMessage) {
		this.type = type;
		this.templateSheetName = templateSheetName;
		this.mappingFieldName = mappingFieldName;
		this.rule = rule;
		this.expression = expression;
		this.errorMessage = errorMessage;
	}

}
