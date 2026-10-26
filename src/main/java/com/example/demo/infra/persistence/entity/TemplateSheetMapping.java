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
 * 範本工作表客製對映定義實體 (Template Sheet Mapping Entity)
 * <p>
 * 位於基礎設施持久化實體層 (Infrastructure Persistence Entity)，映射 {@code template_sheet_mapping} 資料表。<br>
 * 負責定義外部 Excel 試算表實體工作表名稱 ({@link #displayName}，如「使用者名單」、「部門清單」)
 * 與系統內部標準工作表代碼 ({@link #sheetCode}，如 {@code USER}, {@code DEPT}) 之對映關係，
 * 作為第一層工作表級別防腐層 (Sheet-Level Anti-Corruption Layer)。
 * </p>
 */
@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "template_sheet_mapping")
public class TemplateSheetMapping implements Serializable {

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
	 * 系統內部統一工作表代碼 (例如: USER, DEPT, MIS, HR)
	 */
	@Column(name = "sheet_code", nullable = false, length = 64)
	private String sheetCode;

	/**
	 * Excel 實體工作表頁籤顯示文字 (例如: 使用者名單, 部門資料)
	 */
	@Column(name = "display_name", nullable = false, length = 128)
	private String displayName;

	/**
	 * 預期工作表索引順序 (0-based，可為空)
	 */
	@Column(name = "sheet_index")
	private Integer sheetIndex;

	/**
	 * 是否為必備工作表 (Y/N)
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "is_required", nullable = false, length = 1)
	private YesNo isRequired = YesNo.Y;

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
	 * 便捷建構子：快速建立工作表對映實體
	 *
	 * @param templateName 範本名稱
	 * @param sheetCode    內部工作表代碼
	 * @param displayName  外部工作表顯示名稱
	 * @param isRequired   是否必備
	 */
	public TemplateSheetMapping(String templateName, String sheetCode, String displayName, YesNo isRequired) {
		this.templateName = templateName;
		this.sheetCode = sheetCode;
		this.displayName = displayName;
		this.isRequired = isRequired;
		this.activeFlag = YesNo.Y;
		this.createdTime = LocalDateTime.now();
		this.updatedTime = LocalDateTime.now();
	}

	/**
	 * 完整建構子：包含順序索引
	 *
	 * @param templateName 範本名稱
	 * @param sheetCode    內部工作表代碼
	 * @param displayName  外部工作表顯示名稱
	 * @param sheetIndex   工作表索引
	 * @param isRequired   是否必備
	 */
	public TemplateSheetMapping(String templateName, String sheetCode, String displayName, Integer sheetIndex, YesNo isRequired) {
		this(templateName, sheetCode, displayName, isRequired);
		this.sheetIndex = sheetIndex;
	}
}
