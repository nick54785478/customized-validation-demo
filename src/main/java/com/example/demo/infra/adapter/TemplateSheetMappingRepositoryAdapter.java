package com.example.demo.infra.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.example.demo.application.dto.TemplateSheetMappingGottenResult;
import com.example.demo.application.port.out.TemplateSheetMappingRepositoryPort;
import com.example.demo.application.shared.command.CreateTemplateSheetMappingCommand;
import com.example.demo.application.shared.command.UpdateTemplateSheetMappingCommand;
import com.example.demo.application.shared.query.SearchTemplateSheetMappingQuery;
import com.example.demo.infra.assembler.TemplateSheetMappingDtoAssembler;
import com.example.demo.infra.persistence.entity.TemplateSheetMapping;
import com.example.demo.infra.persistence.repository.TemplateSheetMappingRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 範本工作表對映持久化適配器 (Template Sheet Mapping Repository Adapter - Outbound Adapter)
 * <p>
 * 遵循六角形架構規範，實作應用層 Outbound Port {@link TemplateSheetMappingRepositoryPort}。
 * 協調 Spring Data JPA 倉儲 {@link TemplateSheetMappingRepository} 與 DTO 組裝器 {@link TemplateSheetMappingDtoAssembler}，
 * 將持久化實體與應用層模型嚴格隔離。
 * </p>
 */
@Slf4j
@Component
@AllArgsConstructor
public class TemplateSheetMappingRepositoryAdapter implements TemplateSheetMappingRepositoryPort {

	private final TemplateSheetMappingRepository templateSheetMappingRepository;
	private final TemplateSheetMappingDtoAssembler templateSheetMappingDtoAssembler;

	/**
	 * 新增範本工作表對映
	 *
	 * @param command 建立工作表對映指令
	 * @return 建立成功後之對映資料結果
	 */
	@Override
	public TemplateSheetMappingGottenResult save(CreateTemplateSheetMappingCommand command) {
		log.debug("適配器儲存工作表對映實體: command={}", command);
		TemplateSheetMapping entity = templateSheetMappingDtoAssembler.toEntity(command);
		TemplateSheetMapping saved = templateSheetMappingRepository.save(entity);
		return templateSheetMappingDtoAssembler.toResult(saved);
	}

	/**
	 * 更新範本工作表對映
	 *
	 * @param command 更新工作表對映指令
	 * @return 更新後之對映資料結果
	 */
	@Override
	public TemplateSheetMappingGottenResult update(UpdateTemplateSheetMappingCommand command) {
		log.debug("適配器更新工作表對映實體: command={}", command);
		TemplateSheetMapping entity = templateSheetMappingRepository.findById(command.id())
				.orElseThrow(() -> new IllegalStateException("實體不存在: id=" + command.id()));
		templateSheetMappingDtoAssembler.copyProperties(command, entity);
		TemplateSheetMapping updated = templateSheetMappingRepository.save(entity);
		return templateSheetMappingDtoAssembler.toResult(updated);
	}

	/**
	 * 依據主鍵識別碼查詢工作表對映
	 *
	 * @param id 主鍵識別碼
	 * @return 符合條件之對映資料結果 Optional
	 */
	@Override
	public Optional<TemplateSheetMappingGottenResult> findById(Long id) {
		log.debug("適配器依據識別碼查詢工作表對映: id={}", id);
		return templateSheetMappingRepository.findById(id)
				.map(templateSheetMappingDtoAssembler::toResult);
	}

	/**
	 * 依據主鍵識別碼刪除工作表對映
	 *
	 * @param id 主鍵識別碼
	 */
	@Override
	public void deleteById(Long id) {
		log.debug("適配器依據識別碼刪除工作表對映: id={}", id);
		templateSheetMappingRepository.deleteById(id);
	}

	/**
	 * 檢查指定識別碼之工作表對映是否存在
	 *
	 * @param id 主鍵識別碼
	 * @return 若存在回傳 {@code true}，否則回傳 {@code false}
	 */
	@Override
	public boolean existsById(Long id) {
		return templateSheetMappingRepository.existsById(id);
	}

	/**
	 * 依條件動態搜尋工作表對映清單
	 *
	 * @param query 搜尋條件查詢
	 * @return 符合條件之工作表對映清單
	 */
	@Override
	public List<TemplateSheetMappingGottenResult> search(SearchTemplateSheetMappingQuery query) {
		log.debug("適配器動態條件搜尋工作表對映: query={}", query);

		Specification<TemplateSheetMapping> spec = (root, criteriaQuery, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (query != null) {
				if (StringUtils.isNotBlank(query.templateName())) {
					predicates.add(cb.equal(root.get("templateName"), StringUtils.trim(query.templateName())));
				}
				if (StringUtils.isNotBlank(query.sheetCode())) {
					predicates.add(cb.equal(root.get("sheetCode"), StringUtils.trim(query.sheetCode())));
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

		return templateSheetMappingRepository.findAll(spec).stream()
				.map(templateSheetMappingDtoAssembler::toResult)
				.toList();
	}

	/**
	 * 檢查指定範本與工作表代碼之對映是否存在
	 *
	 * @param templateName 範本名稱
	 * @param sheetCode    工作表代碼
	 * @return 若存在回傳 {@code true}，否則回傳 {@code false}
	 */
	@Override
	public boolean existsByTemplateNameAndSheetCode(String templateName, String sheetCode) {
		log.debug("適配器檢查工作表對映是否存在: templateName={}, sheetCode={}", templateName, sheetCode);
		return templateSheetMappingRepository.existsByTemplateNameAndSheetCode(templateName, sheetCode);
	}

	/**
	 * 檢查指定範本與工作表代碼之對映是否存在 (排除指定識別碼)
	 *
	 * @param templateName 範本名稱
	 * @param sheetCode    工作表代碼
	 * @param id           排除之主鍵識別碼
	 * @return 若存在回傳 {@code true}，否則回傳 {@code false}
	 */
	@Override
	public boolean existsByTemplateNameAndSheetCodeAndIdNot(String templateName, String sheetCode, Long id) {
		log.debug("適配器檢查工作表對映是否存在 (排除ID): templateName={}, sheetCode={}, id={}", templateName, sheetCode, id);
		return templateSheetMappingRepository.existsByTemplateNameAndSheetCodeAndIdNot(templateName, sheetCode, id);
	}
}
