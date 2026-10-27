package com.example.demo.application.port.in;

import com.example.demo.application.dto.TemplateFieldMappingGottenResult;
import com.example.demo.application.shared.query.GetTemplateFieldMappingQuery;

/**
 * 依識別碼取得範本欄位對映用例 (Get Template Field Mapping Use Case - Inbound Port)
 * <p>
 * 遵循六角形架構規範，作為應用層對外開放的業務輸入合約。
 * </p>
 */
public interface GetTemplateFieldMappingUseCase {

	/**
	 * 執行取得單一範本欄位對映操作
	 *
	 * @param query 取得欄位對映查詢
	 * @return 對映資料結果
	 */
	TemplateFieldMappingGottenResult get(GetTemplateFieldMappingQuery query);
}
