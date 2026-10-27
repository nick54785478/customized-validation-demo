package com.example.demo.application.port.in;

import com.example.demo.application.shared.command.DeleteTemplateFieldMappingCommand;

/**
 * 刪除範本欄位對映用例 (Delete Template Field Mapping Use Case - Inbound Port)
 * <p>
 * 遵循六角形架構規範，作為應用層對外開放的業務輸入合約。
 * </p>
 */
public interface DeleteTemplateFieldMappingUseCase {

	/**
	 * 執行刪除範本欄位對映操作
	 *
	 * @param command 刪除欄位對映指令
	 */
	void delete(DeleteTemplateFieldMappingCommand command);
}
