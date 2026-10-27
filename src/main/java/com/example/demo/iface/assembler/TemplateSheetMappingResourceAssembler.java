package com.example.demo.iface.assembler;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.demo.application.dto.TemplateSheetMappingGottenResult;
import com.example.demo.application.dto.TemplateSheetMappingSearchedResult;
import com.example.demo.application.shared.command.CreateTemplateSheetMappingCommand;
import com.example.demo.application.shared.command.DeleteTemplateSheetMappingCommand;
import com.example.demo.application.shared.command.UpdateTemplateSheetMappingCommand;
import com.example.demo.application.shared.query.GetTemplateSheetMappingQuery;
import com.example.demo.application.shared.query.SearchTemplateSheetMappingQuery;
import com.example.demo.application.shared.vo.YesNo;
import com.example.demo.iface.resource.in.CreateTemplateSheetMappingResource;
import com.example.demo.iface.resource.in.UpdateTemplateSheetMappingResource;
import com.example.demo.iface.resource.out.TemplateSheetMappingCreatedResource;
import com.example.demo.iface.resource.out.TemplateSheetMappingDeletedResource;
import com.example.demo.iface.resource.out.TemplateSheetMappingRetrievedResource;
import com.example.demo.iface.resource.out.TemplateSheetMappingSearchedResource;
import com.example.demo.iface.resource.out.TemplateSheetMappingUpdatedResource;

/**
 * 範本工作表對映表現層組裝器 (Template Sheet Mapping Resource Assembler)
 * <p>
 * 遵循六角形架構規範，作為表現層 Resource 與應用層 Command / Query / Result 之間的防腐層 (ACL)。
 * 確保 Request / Response Resource 物件保持為純資料載體，所有轉換職責皆在此執行。
 * </p>
 */
@Component
public class TemplateSheetMappingResourceAssembler {

	/**
	 * 將建立請求資源轉換為應用層指令
	 *
	 * @param resource 前端建立工作表對映請求
	 * @return 建立工作表對映業務指令
	 */
	public CreateTemplateSheetMappingCommand toCommand(CreateTemplateSheetMappingResource resource) {
		if (resource == null) {
			return null;
		}
		return new CreateTemplateSheetMappingCommand(
				resource.templateName(),
				resource.sheetCode(),
				resource.displayName(),
				resource.sheetIndex(),
				resource.isRequired() != null ? resource.isRequired() : YesNo.Y,
				resource.activeFlag() != null ? resource.activeFlag() : YesNo.Y);
	}

	/**
	 * 將更新請求資源轉換為應用層指令
	 *
	 * @param id       路徑主鍵識別碼
	 * @param resource 前端更新工作表對映請求
	 * @return 更新工作表對映業務指令
	 */
	public UpdateTemplateSheetMappingCommand toCommand(Long id, UpdateTemplateSheetMappingResource resource) {
		if (resource == null) {
			return null;
		}
		return new UpdateTemplateSheetMappingCommand(
				id,
				resource.templateName(),
				resource.sheetCode(),
				resource.displayName(),
				resource.sheetIndex(),
				resource.isRequired(),
				resource.activeFlag());
	}

	/**
	 * 將路徑主鍵轉換為刪除指令
	 *
	 * @param id 主鍵識別碼
	 * @return 刪除指令
	 */
	public DeleteTemplateSheetMappingCommand toDeleteCommand(Long id) {
		return new DeleteTemplateSheetMappingCommand(id);
	}

	/**
	 * 將路徑主鍵轉換為單筆查詢
	 *
	 * @param id 主鍵識別碼
	 * @return 取得查詢
	 */
	public GetTemplateSheetMappingQuery toGetQuery(Long id) {
		return new GetTemplateSheetMappingQuery(id);
	}

	/**
	 * 將搜尋參數轉換為條件搜尋查詢
	 *
	 * @param templateName 範本名稱
	 * @param sheetCode    工作表代碼
	 * @param displayName  顯示名稱
	 * @param activeFlag   啟用旗標
	 * @return 搜尋查詢條件
	 */
	public SearchTemplateSheetMappingQuery toSearchQuery(
			String templateName,
			String sheetCode,
			String displayName,
			YesNo activeFlag) {
		return new SearchTemplateSheetMappingQuery(templateName, sheetCode, displayName, activeFlag);
	}

	/**
	 * 將查詢結果轉換為建立成功回應資源
	 *
	 * @param result 應用層結果
	 * @return 建立成功回應資源
	 */
	public TemplateSheetMappingCreatedResource toCreatedResource(TemplateSheetMappingGottenResult result) {
		if (result == null) {
			return null;
		}
		return new TemplateSheetMappingCreatedResource(
				result.id(),
				result.templateName(),
				result.sheetCode(),
				result.displayName(),
				result.sheetIndex(),
				result.isRequired(),
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
	public TemplateSheetMappingUpdatedResource toUpdatedResource(TemplateSheetMappingGottenResult result) {
		if (result == null) {
			return null;
		}
		return new TemplateSheetMappingUpdatedResource(
				result.id(),
				result.templateName(),
				result.sheetCode(),
				result.displayName(),
				result.sheetIndex(),
				result.isRequired(),
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
	public TemplateSheetMappingRetrievedResource toRetrievedResource(TemplateSheetMappingGottenResult result) {
		if (result == null) {
			return null;
		}
		return new TemplateSheetMappingRetrievedResource(
				result.id(),
				result.templateName(),
				result.sheetCode(),
				result.displayName(),
				result.sheetIndex(),
				result.isRequired(),
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
	public TemplateSheetMappingSearchedResource toSearchedResource(TemplateSheetMappingSearchedResult result) {
		if (result == null) {
			return new TemplateSheetMappingSearchedResource(Collections.emptyList(), 0);
		}
		List<TemplateSheetMappingRetrievedResource> items = result.items() != null
				? result.items().stream().map(this::toRetrievedResource).toList()
				: Collections.emptyList();
		return new TemplateSheetMappingSearchedResource(items, result.total());
	}

	/**
	 * 轉換為刪除成功回應資源
	 *
	 * @param id 主鍵識別碼
	 * @return 刪除成功回應資源
	 */
	public TemplateSheetMappingDeletedResource toDeletedResource(Long id) {
		return new TemplateSheetMappingDeletedResource(id, "刪除工作表對映成功");
	}
}
