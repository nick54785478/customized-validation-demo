package com.example.demo.application.service;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.dto.TemplateSheetMappingGottenResult;
import com.example.demo.application.dto.TemplateSheetMappingSearchedResult;
import com.example.demo.application.exception.ValidationException;
import com.example.demo.application.port.in.CreateTemplateSheetMappingUseCase;
import com.example.demo.application.port.in.DeleteTemplateSheetMappingUseCase;
import com.example.demo.application.port.in.GetTemplateSheetMappingUseCase;
import com.example.demo.application.port.in.SearchTemplateSheetMappingUseCase;
import com.example.demo.application.port.in.UpdateTemplateSheetMappingUseCase;
import com.example.demo.application.port.out.TemplateSheetMappingRepositoryPort;
import com.example.demo.application.shared.command.CreateTemplateSheetMappingCommand;
import com.example.demo.application.shared.command.DeleteTemplateSheetMappingCommand;
import com.example.demo.application.shared.command.UpdateTemplateSheetMappingCommand;
import com.example.demo.application.shared.query.GetTemplateSheetMappingQuery;
import com.example.demo.application.shared.query.SearchTemplateSheetMappingQuery;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 範本工作表對映應用服務 (Template Sheet Mapping Application Service - Inbound Adapter)
 * <p>
 * 遵循六角形架構規範，實作工作表對映的各項 Inbound Port 用例合約，協調調度 Outbound Port {@link TemplateSheetMappingRepositoryPort}。
 * 負責檢核業務規則、控制事務與處理異常邊界。
 * </p>
 */
@Slf4j
@Service
@AllArgsConstructor
public class TemplateSheetMappingApplicationService implements
		CreateTemplateSheetMappingUseCase,
		UpdateTemplateSheetMappingUseCase,
		DeleteTemplateSheetMappingUseCase,
		GetTemplateSheetMappingUseCase,
		SearchTemplateSheetMappingUseCase {

	private final TemplateSheetMappingRepositoryPort templateSheetMappingRepositoryPort;

	/**
	 * 建立範本工作表對映
	 *
	 * @param command 建立工作表對映指令
	 * @return 建立成功之對映資料
	 */
	@Override
	@Transactional
	public TemplateSheetMappingGottenResult create(CreateTemplateSheetMappingCommand command) {
		log.debug("執行建立工作表對映: command={}", command);
		if (command == null) {
			throw new ValidationException("PARAM_INVALID", "建立工作表對映參數不可為空");
		}
		if (StringUtils.isBlank(command.templateName())) {
			throw new ValidationException("PARAM_INVALID", "範本名稱 (templateName) 為必填欄位");
		}
		if (StringUtils.isBlank(command.sheetCode())) {
			throw new ValidationException("PARAM_INVALID", "工作表代碼 (sheetCode) 為必填欄位");
		}
		if (StringUtils.isBlank(command.displayName())) {
			throw new ValidationException("PARAM_INVALID", "工作表顯示名稱 (displayName) 為必填欄位");
		}

		if (templateSheetMappingRepositoryPort.existsByTemplateNameAndSheetCode(command.templateName(), command.sheetCode())) {
			throw new ValidationException("DUPLICATE_KEY",
					"該範本已存在相同之工作表代碼: templateName=" + command.templateName() + ", sheetCode=" + command.sheetCode());
		}

		return templateSheetMappingRepositoryPort.save(command);
	}

	/**
	 * 更新範本工作表對映
	 *
	 * @param command 更新工作表對映指令
	 * @return 更新後之對映資料
	 */
	@Override
	@Transactional
	public TemplateSheetMappingGottenResult update(UpdateTemplateSheetMappingCommand command) {
		log.debug("執行更新工作表對映: command={}", command);
		if (command == null || command.id() == null || command.id() <= 0) {
			throw new ValidationException("PARAM_INVALID", "更新工作表對映識別碼 (id) 必須大於 0");
		}
		if (!templateSheetMappingRepositoryPort.existsById(command.id())) {
			throw new ValidationException("NOT_FOUND", "找不到欲更新之工作表對映資料: id=" + command.id());
		}
		if (StringUtils.isBlank(command.templateName())) {
			throw new ValidationException("PARAM_INVALID", "範本名稱 (templateName) 為必填欄位");
		}
		if (StringUtils.isBlank(command.sheetCode())) {
			throw new ValidationException("PARAM_INVALID", "工作表代碼 (sheetCode) 為必填欄位");
		}
		if (StringUtils.isBlank(command.displayName())) {
			throw new ValidationException("PARAM_INVALID", "工作表顯示名稱 (displayName) 為必填欄位");
		}

		if (templateSheetMappingRepositoryPort.existsByTemplateNameAndSheetCodeAndIdNot(
				command.templateName(), command.sheetCode(), command.id())) {
			throw new ValidationException("DUPLICATE_KEY",
					"該範本已存在相同之工作表代碼: templateName=" + command.templateName() + ", sheetCode=" + command.sheetCode());
		}

		return templateSheetMappingRepositoryPort.update(command);
	}

	/**
	 * 刪除範本工作表對映
	 *
	 * @param command 刪除工作表對映指令
	 */
	@Override
	@Transactional
	public void delete(DeleteTemplateSheetMappingCommand command) {
		log.debug("執行刪除工作表對映: command={}", command);
		if (command == null || command.id() == null || command.id() <= 0) {
			throw new ValidationException("PARAM_INVALID", "刪除工作表對映識別碼 (id) 必須大於 0");
		}
		if (!templateSheetMappingRepositoryPort.existsById(command.id())) {
			throw new ValidationException("NOT_FOUND", "找不到欲刪除之工作表對映資料: id=" + command.id());
		}
		templateSheetMappingRepositoryPort.deleteById(command.id());
	}

	/**
	 * 依據識別碼取得工作表對映
	 *
	 * @param query 查詢條件
	 * @return 工作表對映結果
	 */
	@Override
	@Transactional(readOnly = true)
	public TemplateSheetMappingGottenResult get(GetTemplateSheetMappingQuery query) {
		log.debug("執行取得工作表對映: query={}", query);
		if (query == null || query.id() == null || query.id() <= 0) {
			throw new ValidationException("PARAM_INVALID", "查詢工作表對映識別碼 (id) 必須大於 0");
		}
		return templateSheetMappingRepositoryPort.findById(query.id())
				.orElseThrow(() -> new ValidationException("NOT_FOUND", "找不到指定之工作表對映資料: id=" + query.id()));
	}

	/**
	 * 條件搜尋範本工作表對映清單
	 *
	 * @param query 搜尋條件
	 * @return 搜尋結果與統計
	 */
	@Override
	@Transactional(readOnly = true)
	public TemplateSheetMappingSearchedResult search(SearchTemplateSheetMappingQuery query) {
		log.debug("執行搜尋工作表對映: query={}", query);
		List<TemplateSheetMappingGottenResult> items = templateSheetMappingRepositoryPort.search(query);
		return new TemplateSheetMappingSearchedResult(items, items.size());
	}
}
