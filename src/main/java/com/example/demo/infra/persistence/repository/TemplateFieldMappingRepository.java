package com.example.demo.infra.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.application.shared.vo.YesNo;
import com.example.demo.infra.persistence.entity.TemplateFieldMapping;

/**
 * 範本欄位客製對映持久化倉儲 (Template Field Mapping Repository)
 * <p>
 * 位於基礎設施持久化層 (Infrastructure Persistence Layer)，負責提供 {@link TemplateFieldMapping} 實體的
 * 資料庫存取與查詢操作。
 * </p>
 */
@Repository
public interface TemplateFieldMappingRepository extends JpaRepository<TemplateFieldMapping, Long> {

	/**
	 * 依據範本名稱與啟用狀態查詢所有欄位對映定義
	 *
	 * @param templateName 範本名稱 (例如: USER_PROFILE)
	 * @param activeFlag   啟用標記 (YesNo.Y / YesNo.N)
	 * @return 欄位對映定義清單
	 */
	List<TemplateFieldMapping> findByTemplateNameAndActiveFlag(String templateName, YesNo activeFlag);

	/**
	 * 依據範本名稱、工作表名稱與啟用狀態查詢欄位對映定義
	 *
	 * @param templateName       範本名稱
	 * @param templateSheetName 工作表名稱
	 * @param activeFlag         啟用標記
	 * @return 該工作表專屬之欄位對映定義清單
	 */
	List<TemplateFieldMapping> findByTemplateNameAndTemplateSheetNameAndActiveFlag(
			String templateName, String templateSheetName, YesNo activeFlag);
}
