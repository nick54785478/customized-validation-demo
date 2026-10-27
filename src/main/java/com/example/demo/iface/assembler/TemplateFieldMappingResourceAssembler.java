package com.example.demo.iface.assembler;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.demo.application.dto.TemplateFieldMappingGottenResult;
import com.example.demo.application.dto.TemplateFieldMappingSearchedResult;
import com.example.demo.application.shared.command.CreateTemplateFieldMappingCommand;
import com.example.demo.application.shared.command.DeleteTemplateFieldMappingCommand;
import com.example.demo.application.shared.command.UpdateTemplateFieldMappingCommand;
import com.example.demo.application.shared.query.GetTemplateFieldMappingQuery;
import com.example.demo.application.shared.query.SearchTemplateFieldMappingQuery;
import com.example.demo.application.shared.vo.YesNo;
import com.example.demo.iface.resource.in.CreateTemplateFieldMappingResource;
import com.example.demo.iface.resource.in.UpdateTemplateFieldMappingResource;
import com.example.demo.iface.resource.out.TemplateFieldMappingCreatedResource;
import com.example.demo.iface.resource.out.TemplateFieldMappingDeletedResource;
import com.example.demo.iface.resource.out.TemplateFieldMappingRetrievedResource;
import com.example.demo.iface.resource.out.TemplateFieldMappingSearchedResource;
import com.example.demo.iface.resource.out.TemplateFieldMappingUpdatedResource;

/**
 * 範本欄位對映表現層組裝器 (Template Field Mapping Resource Assembler)
 * <p>
 * 遵循六角形架構規範，作為表現層 Resource 與應用層 Command / Query / Result 之間的防腐層 (ACL)。
 * </p>
 */
@Component
public class TemplateFieldMappingResourceAssembler {

	/**
	 * 將建立請求資源轉換為應用層指令
	 *
	 * @param resource 前端建立欄位對映請求
	 * @return 建立欄位對映業務指令
	 */
	public CreateTemplateFieldMappingCommand toCommand(CreateTemplateFieldMappingResource resource) {
		if (resource == null) {
			return null;
		}
		return new CreateTemplateFieldMappingCommand(
				resource.templateName(),
				resource.templateSheetName(),
				resource.fieldCode(),
				resource.displayName(),
				resource.columnIndex(),
				resource.isRequired() != null ? resource.isRequired() : YesNo.N,
				resource.dataType() != null ? resource.dataType() : "STRING",
				resource.activeFlag() != null ? resource.activeFlag() : YesNo.Y);
	}

	/**
	 * 將更新請求資源轉換為應用層指令
	 *
	 * @param id       路徑主鍵識別碼
	 * @param resource 前端更新欄位對映請求
	 * @return 更新欄位對映業務指令
	 */
	public UpdateTemplateFieldMappingCommand toCommand(Long id, UpdateTemplateFieldMappingResource resource) {
		if (resource == null) {
			return null;
		}
		return new UpdateTemplateFieldMappingCommand(
				id,
				resource.templateName(),
				resource.templateSheetName(),
				resource.fieldCode(),
				resource.displayName(),
				resource.columnIndex(),
				resource.isRequired(),
				resource.dataType(),
				resource.activeFlag());
	}

	/**
	 * 將路徑主鍵轉換為刪除指令
	 *
	 * @param id 主鍵識別碼
	 * @return 刪除指令
	 */
	public DeleteTemplateFieldMappingCommand toDeleteCommand(Long id) {
		return new DeleteTemplateFieldMappingCommand(id);
	}

	/**
	 * 將路徑主鍵轉換為單筆查詢
	 *
	 * @param id 主鍵識別碼
	 * @return 取得查詢
	 */
	public GetTemplateFieldMappingQuery toGetQuery(Long id) {
		return new GetTemplateFieldMappingQuery(id);
	}

	/**
	 * 將搜尋參數轉換為條件搜尋查詢
	 *
	 * @param templateName       範本名稱
	 * @param templateSheetName 工作表名稱
	 * @param fieldCode          屬性代碼
	 * @param displayName        標題名稱
	 * @param activeFlag         啟用旗標
	 * @return 搜尋查詢條件
	 */
	public SearchTemplateFieldMappingQuery toSearchQuery(
			String templateName,
			String templateSheetName,
			String fieldCode,
			String displayName,
			YesNo activeFlag) {
		return new SearchTemplateFieldMappingQuery(templateName, templateSheetName, fieldCode, displayName, activeFlag);
	}

	/**
	 * 將查詢結果轉換為建立成功回應資源
	 *
	 * @param result 應用層結果
	 * @return 建立成功回應資源
	 */
	public TemplateFieldMappingCreatedResource toCreatedResource(TemplateFieldMappingGottenResult result) {
		if (result == null) {
			return null;
		}
		return new TemplateFieldMappingCreatedResource(
				result.id(),
				result.templateName(),
				result.templateSheetName(),
				result.fieldCode(),
				result.displayName(),
				result.columnIndex(),
				result.isRequired(),
				result.dataType(),
				result.activeFlag(),
				result.createdTime(),
				result.updatedTime());
	}

	/**
	 * 將查詢結果轉換為更新成功回應資源
	 *
	 * @param result 應用層結果
	 * @return 更新成功回應資源
	 */
	public TemplateFieldMappingUpdatedResource toUpdatedResource(TemplateFieldMappingGottenResult result) {
		if (result == null) {
			return null;
		}
		return new TemplateFieldMappingUpdatedResource(
				result.id(),
				result.templateName(),
				result.templateSheetName(),
				result.fieldCode(),
				result.displayName(),
				result.columnIndex(),
				result.isRequired(),
				result.dataType(),
				result.activeFlag(),
				result.createdTime(),
				result.updatedTime());
	}

	/**
	 * 將查詢結果轉換為單筆取得回應資源
	 *
	 * @param result 應用層結果
	 * @return 單筆查詢回應資源
	 */
	public TemplateFieldMappingRetrievedResource toRetrievedResource(TemplateFieldMappingGottenResult result) {
		if (result == null) {
			return null;
		}
		return new TemplateFieldMappingRetrievedResource(
				result.id(),
				result.templateName(),
				result.templateSheetName(),
				result.fieldCode(),
				result.displayName(),
				result.columnIndex(),
				result.isRequired(),
				result.dataType(),
				result.activeFlag(),
				result.createdTime(),
				result.updatedTime());
	}

	/**
	 * 將搜尋結果清單轉換為搜尋回應資源
	 *
	 * @param result 應用層搜尋結果
	 * @return 搜尋回應清單資源
	 */
	public TemplateFieldMappingSearchedResource toSearchedResource(TemplateFieldMappingSearchedResult result) {
		if (result == null) {
			return new TemplateFieldMappingSearchedResource(Collections.emptyList(), 0);
		}
		List<TemplateFieldMappingRetrievedResource> items = result.items() != null
				? result.items().stream().map(this::toRetrievedResource).toList()
				: Collections.emptyList();
		return new TemplateFieldMappingSearchedResource(items, result.total());
	}

	/**
	 * 轉換為刪除成功回應資源
	 *
	 * @param id 主鍵識別碼
	 * @return 刪除成功回應資源
	 */
	public TemplateFieldMappingDeletedResource toDeletedResource(Long id) {
		return new TemplateFieldMappingDeletedResource(id, "刪除欄位對映成功");
	}
}
