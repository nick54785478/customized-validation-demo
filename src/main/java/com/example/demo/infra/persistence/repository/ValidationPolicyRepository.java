package com.example.demo.infra.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.infra.persistence.entity.ValidationPolicy;

/**
 * 範本客製驗證原則持久化倉儲 (Validation Policy Repository)
 * <p>
 * 位於基礎設施持久化層 (Infrastructure Persistence Layer)，負責提供 {@link ValidationPolicy} 實體的
 * 資料庫存取與查詢操作。
 * </p>
 */
@Repository
public interface ValidationPolicyRepository extends JpaRepository<ValidationPolicy, Long> {

	/**
	 * 依據範本名稱查詢所有設定的客製驗證規則清單
	 *
	 * @param templateName 範本名稱 (例如: USER_PROFILE, DEPT_PROFILE)
	 * @return 符合該範本的客製驗證原則實體清單
	 */
	List<ValidationPolicy> findByTemplateName(String templateName);
}
