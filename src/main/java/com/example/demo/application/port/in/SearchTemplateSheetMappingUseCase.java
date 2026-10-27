package com.example.demo.application.port.in;

import com.example.demo.application.dto.TemplateSheetMappingSearchedResult;
import com.example.demo.application.shared.query.SearchTemplateSheetMappingQuery;

/**
 * 條件搜尋範本工作表對映用例 (Search Template Sheet Mapping Use Case - Inbound Port)
 * <p>
 * 遵循六角形架構規範，作為應用層對外開放的業務輸入合約。
 * </p>
 */
public interface SearchTemplateSheetMappingUseCase {

	/**
	 * 依條件執行搜尋範本工作表對映操作
	 *
	 * @param query 搜尋條件查詢
	 * @return 搜尋結果集合與筆數
	 */
	TemplateSheetMappingSearchedResult search(SearchTemplateSheetMappingQuery query);
}
