package com.example.demo.infra.assembler;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.example.demo.application.dto.TemplateSheetMappingGottenResult;
import com.example.demo.application.shared.command.CreateTemplateSheetMappingCommand;
import com.example.demo.application.shared.command.UpdateTemplateSheetMappingCommand;
import com.example.demo.application.shared.vo.YesNo;
import com.example.demo.infra.persistence.entity.TemplateSheetMapping;

/**
 * 範本工作表對映 DTO 組裝器 (Template Sheet Mapping DTO Assembler)
 * <p>
 * 遵循六角形架構規範，作為基礎設施持久化層實體與應用層純資料載體 (DTO) 之間的轉換器。
 * 避免在 DTO 或實體內部污染轉換邏輯，落實單一職責原則。
 * </p>
 */
@Component
public class TemplateSheetMappingDtoAssembler {

	/**
	 * 將實體物件轉換為應用層查詢結果 DTO
	 *
	 * @param entity 資料庫實體物件
	 * @return 應用層結果 DTO
	 */
	public TemplateSheetMappingGottenResult toResult(TemplateSheetMapping entity) {
		if (entity == null) {
			return null;
		}
		return new TemplateSheetMappingGottenResult(
				entity.getId(),
				entity.getTemplateName(),
				entity.getSheetCode(),
				entity.getDisplayName(),
				entity.getSheetIndex(),
				entity.getIsRequired(),
				entity.getActiveFlag(),
				entity.getCreatedTime(),
				entity.getUpdatedTime());
	}

	/**
	 * 將建立指令轉換為持久化實體物件
	 *
	 * @param command 建立工作表對映指令
	 * @return 持久化實體物件
	 */
	public TemplateSheetMapping toEntity(CreateTemplateSheetMappingCommand command) {
		if (command == null) {
			return null;
		}
		TemplateSheetMapping entity = new TemplateSheetMapping();
		entity.setTemplateName(command.templateName());
		entity.setSheetCode(command.sheetCode());
		entity.setDisplayName(command.displayName());
		entity.setSheetIndex(command.sheetIndex());
		entity.setIsRequired(command.isRequired() != null ? command.isRequired() : YesNo.Y);
		entity.setActiveFlag(command.activeFlag() != null ? command.activeFlag() : YesNo.Y);
		entity.setCreatedTime(LocalDateTime.now());
		entity.setUpdatedTime(LocalDateTime.now());
		return entity;
	}

	/**
	 * 將更新指令屬性複製並覆蓋至現有實體物件
	 *
	 * @param command 更新工作表對映指令
	 * @param entity  既有之實體物件
	 */
	public void copyProperties(UpdateTemplateSheetMappingCommand command, TemplateSheetMapping entity) {
		if (command == null || entity == null) {
			return;
		}
		entity.setTemplateName(command.templateName());
		entity.setSheetCode(command.sheetCode());
		entity.setDisplayName(command.displayName());
		entity.setSheetIndex(command.sheetIndex());
		if (command.isRequired() != null) {
			entity.setIsRequired(command.isRequired());
		}
		if (command.activeFlag() != null) {
			entity.setActiveFlag(command.activeFlag());
		}
		entity.setUpdatedTime(LocalDateTime.now());
	}
}
