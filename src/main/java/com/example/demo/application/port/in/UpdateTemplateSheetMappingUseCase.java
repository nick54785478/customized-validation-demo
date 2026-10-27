package com.example.demo.application.port.in;

import com.example.demo.application.dto.TemplateSheetMappingGottenResult;
import com.example.demo.application.shared.command.UpdateTemplateSheetMappingCommand;

/**
 * 更新範本工作表對映用例 (Update Template Sheet Mapping Use Case - Inbound Port)
 * <p>
 * 遵循六角形架構規範，作為應用層對外開放的業務輸入合約。
 * </p>
 */
public interface UpdateTemplateSheetMappingUseCase {

	/**
	 * 執行更新範本工作表對映操作
	 *
	 * @param command 更新工作表對映指令
	 * @return 更新後之對映資料結果
	 */
	TemplateSheetMappingGottenResult update(UpdateTemplateSheetMappingCommand command);
}
