package com.example.demo.infra.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.example.demo.application.dto.TemplateFieldMappingGottenResult;
import com.example.demo.application.port.out.TemplateFieldMappingRepositoryPort;
import com.example.demo.application.shared.command.CreateTemplateFieldMappingCommand;
import com.example.demo.application.shared.command.UpdateTemplateFieldMappingCommand;
import com.example.demo.application.shared.query.SearchTemplateFieldMappingQuery;
import com.example.demo.infra.assembler.TemplateFieldMappingDtoAssembler;
import com.example.demo.infra.persistence.entity.TemplateFieldMapping;
import com.example.demo.infra.persistence.repository.TemplateFieldMappingRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 範本欄位對映持久化適配器 (Template Field Mapping Repository Adapter - Outbound Adapter)
 * <p>
 * 遵循六角形架構規範，實作應用層 Outbound Port {@link TemplateFieldMappingRepositoryPort}。
 * 協調 Spring Data JPA 倉儲 {@link TemplateFieldMappingRepository} 與 DTO 組裝器 {@link TemplateFieldMappingDtoAssembler}，
 * 將持久化實體與應用層模型嚴格隔離。
 * </p>
 */
@Slf4j
@Component
@AllArgsConstructor
public class TemplateFieldMappingRepositoryAdapter implements TemplateFieldMappingRepositoryPort {

	private final TemplateFieldMappingRepository templateFieldMappingRepository;
	private final TemplateFieldMappingDtoAssembler templateFieldMappingDtoAssembler;

	/**
	 * 新增範本欄位對映
	 *
	 * @param command 建立欄位對映指令
	 * @return 建立成功後之對映資料結果
	 */
	@Override
	public TemplateFieldMappingGottenResult save(CreateTemplateFieldMappingCommand command) {
		log.debug("適配器儲存欄位對映實體: command={}", command);
		TemplateFieldMapping entity = templateFieldMappingDtoAssembler.toEntity(command);
		TemplateFieldMapping saved = templateFieldMappingRepository.save(entity);
		return templateFieldMappingDtoAssembler.toResult(saved);
	}

	/**
	 * 更新範本欄位對映
	 *
	 * @param command 更新欄位對映指令
	 * @return 更新後之對映資料結果
	 */
	@Override
	public TemplateFieldMappingGottenResult update(UpdateTemplateFieldMappingCommand command) {
		log.debug("適配器更新欄位對映實體: command={}", command);
		TemplateFieldMapping entity = templateFieldMappingRepository.findById(command.id())
				.orElseThrow(() -> new IllegalStateException("實體不存在: id=" + command.id()));
		templateFieldMappingDtoAssembler.copyProperties(command, entity);
		TemplateFieldMapping updated = templateFieldMappingRepository.save(entity);
		return templateFieldMappingDtoAssembler.toResult(updated);
	}

	/**
	 * 依據主鍵識別碼查詢欄位對映
	 *
	 * @param id 主鍵識別碼
	 * @return 符合條件之對映資料結果 Optional
	 */
	@Override
	public Optional<TemplateFieldMappingGottenResult> findById(Long id) {
		log.debug("適配器依據識別碼查詢欄位對映: id={}", id);
		return templateFieldMappingRepository.findById(id)
				.map(templateFieldMappingDtoAssembler::toResult);
	}

	/**
	 * 依據主鍵識別碼刪除欄位對映
	 *
	 * @param id 主鍵識別碼
	 */
	@Override
	public void deleteById(Long id) {
		log.debug("適配器依據識別碼刪除欄位對映: id={}", id);
		templateFieldMappingRepository.deleteById(id);
	}

	/**
	 * 檢查指定識別碼之欄位對映是否存在
	 *
	 * @param id 主鍵識別碼
	 * @return 若存在回傳 {@code true}，否則回傳 {@code false}
	 */
	@Override
	public boolean existsById(Long id) {
		return templateFieldMappingRepository.existsById(id);
	}

	/**
	 * 依條件動態搜尋欄位對映清單
	 *
	 * @param query 搜尋條件查詢
	 * @return 符合條件之欄位對映清單
	 */
	@Override
	public List<TemplateFieldMappingGottenResult> search(SearchTemplateFieldMappingQuery query) {
		log.debug("適配器動態條件搜尋欄位對映: query={}", query);

		Specification<TemplateFieldMapping> spec = (root, criteriaQuery, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (query != null) {
				if (StringUtils.isNotBlank(query.templateName())) {
					predicates.add(cb.equal(root.get("templateName"), StringUtils.trim(query.templateName())));
				}
				if (StringUtils.isNotBlank(query.templateSheetName())) {
					predicates.add(cb.equal(root.get("templateSheetName"), StringUtils.trim(query.templateSheetName())));
				}
				if (StringUtils.isNotBlank(query.fieldCode())) {
					predicates.add(cb.equal(root.get("fieldCode"), StringUtils.trim(query.fieldCode())));
				}
				if (StringUtils.isNotBlank(query.displayName())) {
					predicates.add(cb.like(root.get("displayName"), "%" + StringUtils.trim(query.displayName()) + "%"));
				}
				if (query.activeFlag() != null) {
					predicates.add(cb.equal(root.get("activeFlag"), query.activeFlag()));
				}
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};

		return templateFieldMappingRepository.findAll(spec).stream()
				.map(templateFieldMappingDtoAssembler::toResult)
				.toList();
	}

	/**
	 * 檢查指定範本、工作表與欄位代碼之對映是否存在
	 *
	 * @param templateName       範本名稱
	 * @param templateSheetName 工作表代碼
	 * @param fieldCode          屬性代碼
	 * @return 若存在回傳 {@code true}，否則回傳 {@code false}
	 */
	@Override
	public boolean existsByTemplateNameAndTemplateSheetNameAndFieldCode(
			String templateName, String templateSheetName, String fieldCode) {
		log.debug("適配器檢查欄位對映是否存在: templateName={}, sheet={}, fieldCode={}", templateName, templateSheetName, fieldCode);
		return templateFieldMappingRepository.existsByTemplateNameAndTemplateSheetNameAndFieldCode(
				templateName, templateSheetName, fieldCode);
	}

	/**
	 * 檢查指定範本、工作表與欄位代碼之對映是否存在 (排除指定識別碼)
	 *
	 * @param templateName       範本名稱
	 * @param templateSheetName 工作表代碼
	 * @param fieldCode          屬性代碼
	 * @param id                 排除之主鍵識別碼
	 * @return 若存在回傳 {@code true}，否則回傳 {@code false}
	 */
	@Override
	public boolean existsByTemplateNameAndTemplateSheetNameAndFieldCodeAndIdNot(
			String templateName, String templateSheetName, String fieldCode, Long id) {
		log.debug("適配器檢查欄位對映是否存在 (排除ID): templateName={}, sheet={}, fieldCode={}, id={}",
				templateName, templateSheetName, fieldCode, id);
		return templateFieldMappingRepository.existsByTemplateNameAndTemplateSheetNameAndFieldCodeAndIdNot(
				templateName, templateSheetName, fieldCode, id);
	}
}
