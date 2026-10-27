package com.example.demo.application.port.in;

import com.example.demo.application.dto.TemplateSheetMappingGottenResult;
import com.example.demo.application.shared.query.GetTemplateSheetMappingQuery;

/**
 * 依識別碼取得範本工作表對映用例 (Get Template Sheet Mapping Use Case - Inbound Port)
 * <p>
 * 遵循六角形架構規範，作為應用層對外開放的業務輸入合約。
 * </p>
 */
public interface GetTemplateSheetMappingUseCase {

	/**
	 * 執行取得單一範本工作表對映操作
	 *
	 * @param query 取得工作表對映查詢
	 * @return 對映資料結果
	 */
	TemplateSheetMappingGottenResult get(GetTemplateSheetMappingQuery query);
}
