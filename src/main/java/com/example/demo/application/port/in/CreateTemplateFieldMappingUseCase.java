package com.example.demo.application.port.in;

import com.example.demo.application.dto.TemplateFieldMappingGottenResult;
import com.example.demo.application.shared.command.CreateTemplateFieldMappingCommand;

/**
 * 建立範本欄位對映用例 (Create Template Field Mapping Use Case - Inbound Port)
 * <p>
 * 遵循六角形架構規範，作為應用層對外開放的業務輸入合約。
 * </p>
 */
public interface CreateTemplateFieldMappingUseCase {

	/**
	 * 執行建立範本欄位對映操作
	 *
	 * @param command 建立欄位對映指令
	 * @return 建立成功後之對映資料結果
	 */
	TemplateFieldMappingGottenResult create(CreateTemplateFieldMappingCommand command);
}
