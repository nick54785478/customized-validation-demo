package com.example.demo.application.service;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.dto.TemplateFieldMappingGottenResult;
import com.example.demo.application.dto.TemplateFieldMappingSearchedResult;
import com.example.demo.application.exception.ValidationException;
import com.example.demo.application.port.in.CreateTemplateFieldMappingUseCase;
import com.example.demo.application.port.in.DeleteTemplateFieldMappingUseCase;
import com.example.demo.application.port.in.GetTemplateFieldMappingUseCase;
import com.example.demo.application.port.in.SearchTemplateFieldMappingUseCase;
import com.example.demo.application.port.in.UpdateTemplateFieldMappingUseCase;
import com.example.demo.application.port.out.TemplateFieldMappingRepositoryPort;
import com.example.demo.application.shared.command.CreateTemplateFieldMappingCommand;
import com.example.demo.application.shared.command.DeleteTemplateFieldMappingCommand;
import com.example.demo.application.shared.command.UpdateTemplateFieldMappingCommand;
import com.example.demo.application.shared.query.GetTemplateFieldMappingQuery;
import com.example.demo.application.shared.query.SearchTemplateFieldMappingQuery;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 範本欄位對映應用服務 (Template Field Mapping Application Service - Inbound Adapter)
 * <p>
 * 遵循六角形架構規範，實作欄位對映的各項 Inbound Port 用例合約，協調調度 Outbound Port {@link TemplateFieldMappingRepositoryPort}。
 * 負責檢核業務規則、控制事務與處理異常邊界。
 * </p>
 */
@Slf4j
@Service
@AllArgsConstructor
public class TemplateFieldMappingApplicationService implements
		CreateTemplateFieldMappingUseCase,
		UpdateTemplateFieldMappingUseCase,
		DeleteTemplateFieldMappingUseCase,
		GetTemplateFieldMappingUseCase,
		SearchTemplateFieldMappingUseCase {

	private final TemplateFieldMappingRepositoryPort templateFieldMappingRepositoryPort;

	/**
	 * 建立範本欄位對映
	 *
	 * @param command 建立欄位對映指令
	 * @return 建立成功之對映資料
	 */
	@Override
	@Transactional
	public TemplateFieldMappingGottenResult create(CreateTemplateFieldMappingCommand command) {
		log.debug("執行建立欄位對映: command={}", command);
		if (command == null) {
			throw new ValidationException("PARAM_INVALID", "建立欄位對映參數不可為空");
		}
		if (StringUtils.isBlank(command.templateName())) {
			throw new ValidationException("PARAM_INVALID", "範本名稱 (templateName) 為必填欄位");
		}
		if (StringUtils.isBlank(command.templateSheetName())) {
			throw new ValidationException("PARAM_INVALID", "所屬工作表名稱 (templateSheetName) 為必填欄位");
		}
		if (StringUtils.isBlank(command.fieldCode())) {
			throw new ValidationException("PARAM_INVALID", "欄位屬性代碼 (fieldCode) 為必填欄位");
		}
		if (StringUtils.isBlank(command.displayName())) {
			throw new ValidationException("PARAM_INVALID", "欄位顯示名稱 (displayName) 為必填欄位");
		}

		if (templateFieldMappingRepositoryPort.existsByTemplateNameAndTemplateSheetNameAndFieldCode(
				command.templateName(), command.templateSheetName(), command.fieldCode())) {
			throw new ValidationException("DUPLICATE_KEY", "該工作表已存在相同之欄位代碼: templateName="
					+ command.templateName() + ", sheet=" + command.templateSheetName() + ", fieldCode=" + command.fieldCode());
		}

		return templateFieldMappingRepositoryPort.save(command);
	}

	/**
	 * 更新範本欄位對映
	 *
	 * @param command 更新欄位對映指令
	 * @return 更新後之對映資料
	 */
	@Override
	@Transactional
	public TemplateFieldMappingGottenResult update(UpdateTemplateFieldMappingCommand command) {
		log.debug("執行更新欄位對映: command={}", command);
		if (command == null || command.id() == null || command.id() <= 0) {
			throw new ValidationException("PARAM_INVALID", "更新欄位對映識別碼 (id) 必須大於 0");
		}
		if (!templateFieldMappingRepositoryPort.existsById(command.id())) {
			throw new ValidationException("NOT_FOUND", "找不到欲更新之欄位對映資料: id=" + command.id());
		}
		if (StringUtils.isBlank(command.templateName())) {
			throw new ValidationException("PARAM_INVALID", "範本名稱 (templateName) 為必填欄位");
		}
		if (StringUtils.isBlank(command.templateSheetName())) {
			throw new ValidationException("PARAM_INVALID", "所屬工作表名稱 (templateSheetName) 為必填欄位");
		}
		if (StringUtils.isBlank(command.fieldCode())) {
			throw new ValidationException("PARAM_INVALID", "欄位屬性代碼 (fieldCode) 為必填欄位");
		}
		if (StringUtils.isBlank(command.displayName())) {
			throw new ValidationException("PARAM_INVALID", "欄位顯示名稱 (displayName) 為必填欄位");
		}

		if (templateFieldMappingRepositoryPort.existsByTemplateNameAndTemplateSheetNameAndFieldCodeAndIdNot(
				command.templateName(), command.templateSheetName(), command.fieldCode(), command.id())) {
			throw new ValidationException("DUPLICATE_KEY", "該工作表已存在相同之欄位代碼: templateName="
					+ command.templateName() + ", sheet=" + command.templateSheetName() + ", fieldCode=" + command.fieldCode());
		}

		return templateFieldMappingRepositoryPort.update(command);
	}

	/**
	 * 刪除範本欄位對映
	 *
	 * @param command 刪除欄位對映指令
	 */
	@Override
	@Transactional
	public void delete(DeleteTemplateFieldMappingCommand command) {
		log.debug("執行刪除欄位對映: command={}", command);
		if (command == null || command.id() == null || command.id() <= 0) {
			throw new ValidationException("PARAM_INVALID", "刪除欄位對映識別碼 (id) 必須大於 0");
		}
		if (!templateFieldMappingRepositoryPort.existsById(command.id())) {
			throw new ValidationException("NOT_FOUND", "找不到欲刪除之欄位對映資料: id=" + command.id());
		}
		templateFieldMappingRepositoryPort.deleteById(command.id());
	}

	/**
	 * 依據識別碼取得欄位對映
	 *
	 * @param query 查詢條件
	 * @return 欄位對映結果
	 */
	@Override
	@Transactional(readOnly = true)
	public TemplateFieldMappingGottenResult get(GetTemplateFieldMappingQuery query) {
		log.debug("執行取得欄位對映: query={}", query);
		if (query == null || query.id() == null || query.id() <= 0) {
			throw new ValidationException("PARAM_INVALID", "查詢欄位對映識別碼 (id) 必須大於 0");
		}
		return templateFieldMappingRepositoryPort.findById(query.id())
				.orElseThrow(() -> new ValidationException("NOT_FOUND", "找不到指定之欄位對映資料: id=" + query.id()));
	}

	/**
	 * 條件搜尋範本欄位對映清單
	 *
	 * @param query 搜尋條件
	 * @return 搜尋結果與統計
	 */
	@Override
	@Transactional(readOnly = true)
	public TemplateFieldMappingSearchedResult search(SearchTemplateFieldMappingQuery query) {
		log.debug("執行搜尋欄位對映: query={}", query);
		List<TemplateFieldMappingGottenResult> items = templateFieldMappingRepositoryPort.search(query);
		return new TemplateFieldMappingSearchedResult(items, items.size());
	}
}
