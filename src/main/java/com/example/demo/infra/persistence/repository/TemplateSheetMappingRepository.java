package com.example.demo.infra.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.example.demo.application.shared.vo.YesNo;
import com.example.demo.infra.persistence.entity.TemplateSheetMapping;

/**
 * 範本工作表客製對映持久化倉儲 (Template Sheet Mapping Repository - Infrastructure Repository)
 * <p>
 * 位於基礎設施層持久化模組，提供 {@link TemplateSheetMapping} 實體之資料庫存取操作。
 * </p>
 */
@Repository
public interface TemplateSheetMappingRepository extends
		JpaRepository<TemplateSheetMapping, Long>,
		JpaSpecificationExecutor<TemplateSheetMapping> {

	/**
	 * 依據範本唯一名稱與啟用旗標查詢所有有效之工作表對映定義
	 *
	 * @param templateName 範本唯一識別名稱 (例如: "USER_PROFILE")
	 * @param activeFlag   啟用旗標 (YesNo.Y / YesNo.N)
	 * @return 符合條件之工作表對映定義清單
	 */
	List<TemplateSheetMapping> findByTemplateNameAndActiveFlag(String templateName, YesNo activeFlag);

	/**
	 * 依據範本唯一名稱查詢所有工作表對映定義
	 *
	 * @param templateName 範本唯一識別名稱
	 * @return 該範本所屬之所有工作表對映定義清單
	 */
	List<TemplateSheetMapping> findByTemplateName(String templateName);

	/**
	 * 檢查指定範本與工作表代碼之對映是否存在
	 *
	 * @param templateName 範本名稱
	 * @param sheetCode    工作表代碼
	 * @return 若存在回傳 {@code true}，否則回傳 {@code false}
	 */
	boolean existsByTemplateNameAndSheetCode(String templateName, String sheetCode);

	/**
	 * 檢查指定範本與工作表代碼之對映是否存在 (排除指定主鍵識別碼)
	 *
	 * @param templateName 範本名稱
	 * @param sheetCode    工作表代碼
	 * @param id           排除之主鍵識別碼
	 * @return 若存在回傳 {@code true}，否則回傳 {@code false}
	 */
	boolean existsByTemplateNameAndSheetCodeAndIdNot(String templateName, String sheetCode, Long id);
}
