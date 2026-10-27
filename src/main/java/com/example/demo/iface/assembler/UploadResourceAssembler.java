package com.example.demo.iface.assembler;

import org.springframework.stereotype.Component;

import com.example.demo.iface.resource.in.UploadTemplateResource;
import com.example.demo.application.shared.command.UploadTemplateCommand;

/**
 * 範本上傳表現層組裝器 (Upload Resource Assembler)
 * <p>
 * 位於表現層與應用層之間的防腐層 (Anti-Corruption Layer - ACL)。
 * 負責將表現層傳入的請求純資料載體 {@link UploadTemplateResource} 轉換為應用層指令 {@link UploadTemplateCommand}，
 * 確保 Resource 物件不污染內部層級。
 * </p>
 */
@Component
public class UploadResourceAssembler {

	/**
	 * 將上傳範本之 Request Resource 轉換為 UploadTemplateCommand
	 *
	 * @param resource 前端傳入之上傳請求資源
	 * @return 應用層範本上傳指令
	 */
	public UploadTemplateCommand toCommand(UploadTemplateResource resource) {
		if (resource == null) {
			return null;
		}
		return new UploadTemplateCommand(
				resource.name(),
				resource.type(),
				resource.fileType(),
				resource.filePath(),
				resource.fileName());
	}
}
