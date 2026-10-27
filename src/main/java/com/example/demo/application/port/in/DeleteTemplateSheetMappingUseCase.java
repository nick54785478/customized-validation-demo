package com.example.demo.application.port.in;

import com.example.demo.application.shared.command.DeleteTemplateSheetMappingCommand;

/**
 * 刪除範本工作表對映用例 (Delete Template Sheet Mapping Use Case - Inbound Port)
 * <p>
 * 遵循六角形架構規範，作為應用層對外開放的業務輸入合約。
 * </p>
 */
public interface DeleteTemplateSheetMappingUseCase {

	/**
	 * 執行刪除範本工作表對映操作
	 *
	 * @param command 刪除工作表對映指令
	 */
	void delete(DeleteTemplateSheetMappingCommand command);
}
