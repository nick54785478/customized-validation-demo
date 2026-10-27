package com.example.demo.application.port.in;

import com.example.demo.application.dto.TemplateFieldMappingSearchedResult;
import com.example.demo.application.shared.query.SearchTemplateFieldMappingQuery;

/**
 * 條件搜尋範本欄位對映用例 (Search Template Field Mapping Use Case - Inbound Port)
 * <p>
 * 遵循六角形架構規範，作為應用層對外開放的業務輸入合約。
 * </p>
 */
public interface SearchTemplateFieldMappingUseCase {

	/**
	 * 依條件執行搜尋範本欄位對映操作
	 *
	 * @param query 搜尋條件查詢
	 * @return 搜尋結果集合與筆數
	 */
	TemplateFieldMappingSearchedResult search(SearchTemplateFieldMappingQuery query);
}
