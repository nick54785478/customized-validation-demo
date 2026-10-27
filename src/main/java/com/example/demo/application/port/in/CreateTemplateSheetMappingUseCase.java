package com.example.demo.application.port.in;

import com.example.demo.application.dto.TemplateSheetMappingGottenResult;
import com.example.demo.application.shared.command.CreateTemplateSheetMappingCommand;

/**
 * 建立範本工作表對映用例 (Create Template Sheet Mapping Use Case - Inbound Port)
 * <p>
 * 遵循六角形架構規範，作為應用層對外開放的業務輸入合約。
 * </p>
 */
public interface CreateTemplateSheetMappingUseCase {

	/**
	 * 執行建立範本工作表對映操作
	 *
	 * @param command 建立工作表對映指令
	 * @return 建立成功後之對映資料結果
	 */
	TemplateSheetMappingGottenResult create(CreateTemplateSheetMappingCommand command);
}
