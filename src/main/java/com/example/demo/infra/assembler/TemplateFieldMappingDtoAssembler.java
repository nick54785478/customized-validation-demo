package com.example.demo.infra.assembler;

import java.time.LocalDateTime;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.example.demo.application.dto.TemplateFieldMappingGottenResult;
import com.example.demo.application.shared.command.CreateTemplateFieldMappingCommand;
import com.example.demo.application.shared.command.UpdateTemplateFieldMappingCommand;
import com.example.demo.application.shared.vo.YesNo;
import com.example.demo.infra.persistence.entity.TemplateFieldMapping;

/**
 * 範本欄位對映 DTO 組裝器 (Template Field Mapping DTO Assembler)
 * <p>
 * 遵循六角形架構規範，作為基礎設施持久化層實體與應用層純資料載體 (DTO) 之間的轉換器。
 * </p>
 */
@Component
public class TemplateFieldMappingDtoAssembler {

	/**
	 * 將實體物件轉換為應用層查詢結果 DTO
	 *
	 * @param entity 資料庫實體物件
	 * @return 應用層結果 DTO
	 */
	public TemplateFieldMappingGottenResult toResult(TemplateFieldMapping entity) {
		if (entity == null) {
			return null;
		}
		return new TemplateFieldMappingGottenResult(
				entity.getId(),
				entity.getTemplateName(),
				entity.getTemplateSheetName(),
				entity.getFieldCode(),
				entity.getDisplayName(),
				entity.getColumnIndex(),
				entity.getIsRequired(),
				entity.getDataType(),
				entity.getActiveFlag(),
				entity.getCreatedTime(),
				entity.getUpdatedTime());
	}

	/**
	 * 將建立指令轉換為持久化實體物件
	 *
	 * @param command 建立欄位對映指令
	 * @return 持久化實體物件
	 */
	public TemplateFieldMapping toEntity(CreateTemplateFieldMappingCommand command) {
		if (command == null) {
			return null;
		}
		TemplateFieldMapping entity = new TemplateFieldMapping();
		entity.setTemplateName(command.templateName());
		entity.setTemplateSheetName(command.templateSheetName());
		entity.setFieldCode(command.fieldCode());
		entity.setDisplayName(command.displayName());
		entity.setColumnIndex(command.columnIndex());
		entity.setIsRequired(command.isRequired() != null ? command.isRequired() : YesNo.N);
		entity.setDataType(StringUtils.isNotBlank(command.dataType()) ? command.dataType() : "STRING");
		entity.setActiveFlag(command.activeFlag() != null ? command.activeFlag() : YesNo.Y);
		entity.setCreatedTime(LocalDateTime.now());
		entity.setUpdatedTime(LocalDateTime.now());
		return entity;
	}

	/**
	 * 將更新指令屬性複製並覆蓋至現有實體物件
	 *
	 * @param command 更新欄位對映指令
	 * @param entity  既有之實體物件
	 */
	public void copyProperties(UpdateTemplateFieldMappingCommand command, TemplateFieldMapping entity) {
		if (command == null || entity == null) {
			return;
		}
		entity.setTemplateName(command.templateName());
		entity.setTemplateSheetName(command.templateSheetName());
		entity.setFieldCode(command.fieldCode());
		entity.setDisplayName(command.displayName());
		entity.setColumnIndex(command.columnIndex());
		if (command.isRequired() != null) {
			entity.setIsRequired(command.isRequired());
		}
		if (StringUtils.isNotBlank(command.dataType())) {
			entity.setDataType(command.dataType());
		}
		if (command.activeFlag() != null) {
			entity.setActiveFlag(command.activeFlag());
		}
		entity.setUpdatedTime(LocalDateTime.now());
	}
}
