package com.example.demo.infra.persistence.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

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
import lombok.Setter;
import lombok.ToString;

/**
 * 範本欄位客製對映定義實體 (Template Field Mapping Entity)
 * <p>
 * 位於基礎設施持久化實體層 (Infrastructure Persistence Entity)，映射 {@code template_field_mapping} 資料表。<br>
 * 負責定義與儲存外部 Excel 試算表實體標題文字 ({@link #displayName}，如「姓名」、「身分證字號」)
 * 與系統內部標準屬性代碼 ({@link #fieldCode}，如 {@code name}, {@code nationId}) 之對映關係，
 * 同時支援定義欄位預期順序、必備性檢查與資料型別。
 * </p>
 */
@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "template_field_mapping")
public class TemplateFieldMapping implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 主鍵識別碼
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 範本唯一識別名稱 (例如: USER_PROFILE, DEPT_PROFILE)
	 */
	@Column(name = "template_name", nullable = false, length = 64)
	private String templateName;

	/**
	 * 範本所屬工作表名稱 (Sheet Name，例如: USER, DEPT)
	 */
	@Column(name = "template_sheet_name", nullable = false, length = 64)
	private String templateSheetName;

	/**
	 * 系統內部統一屬性代碼 (CamelCase，例如: name, age, nationId)
	 */
	@Column(name = "field_code", nullable = false, length = 64)
	private String fieldCode;

	/**
	 * Excel 實體標題顯示文字 (例如: 姓名, 年齡, 身分證字號)
	 */
	@Column(name = "display_name", nullable = false, length = 128)
	private String displayName;

	/**
	 * 預期欄位順序 (1-based，可為空)
	 */
	@Column(name = "column_index")
	private Integer columnIndex;

	/**
	 * 是否為必備欄位 (Y/N)
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "is_required", nullable = false, length = 1)
	private YesNo isRequired = YesNo.N;

	/**
	 * 資料型別 (STRING, NUMBER, DATE...)
	 */
	@Column(name = "data_type", nullable = false, length = 32)
	private String dataType = "STRING";

	/**
	 * 啟用註記 (Y/N)
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "active_flag", nullable = false, length = 1)
	private YesNo activeFlag = YesNo.Y;

	/**
	 * 建立時間
	 */
	@Column(name = "created_time", nullable = false, updatable = false)
	private LocalDateTime createdTime = LocalDateTime.now();

	/**
	 * 更新時間
	 */
	@Column(name = "updated_time", nullable = false)
	private LocalDateTime updatedTime = LocalDateTime.now();

	/**
	 * 便捷建構子：快速建立欄位對照關係
	 *
	 * @param templateName       範本名稱
	 * @param templateSheetName 工作表名稱
	 * @param fieldCode          內部代碼
	 * @param displayName        外部顯示名稱
	 * @param isRequired         是否必填
	 */
	public TemplateFieldMapping(String templateName, String templateSheetName, String fieldCode, String displayName, YesNo isRequired) {
		this.templateName = templateName;
		this.templateSheetName = templateSheetName;
		this.fieldCode = fieldCode;
		this.displayName = displayName;
		this.isRequired = isRequired;
		this.activeFlag = YesNo.Y;
		this.createdTime = LocalDateTime.now();
		this.updatedTime = LocalDateTime.now();
	}
}
