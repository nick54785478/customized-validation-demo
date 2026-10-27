package com.example.demo.application.port.out;

import java.util.List;
import java.util.Optional;

import com.example.demo.application.dto.TemplateFieldMappingGottenResult;
import com.example.demo.application.shared.command.CreateTemplateFieldMappingCommand;
import com.example.demo.application.shared.command.UpdateTemplateFieldMappingCommand;
import com.example.demo.application.shared.query.SearchTemplateFieldMappingQuery;

/**
 * 範本欄位對映持久化輸出連接埠介面 (Template Field Mapping Repository Port - Outbound Port)
 * <p>
 * 遵循六角形架構規範，本介面定義應用層對基礎設施持久化層所要求的合約。
 * 傳參與回傳型別嚴格使用 Command、Query、DTO 與基礎型別，零 ORM 實體等技術類物件侵入。
 * </p>
 */
public interface TemplateFieldMappingRepositoryPort {

	/**
	 * 新增範本欄位對映
	 *
	 * @param command 建立欄位對映指令
	 * @return 建立成功後之對映資料結果
	 */
	TemplateFieldMappingGottenResult save(CreateTemplateFieldMappingCommand command);

	/**
	 * 更新範本欄位對映
	 *
	 * @param command 更新欄位對映指令
	 * @return 更新後之對映資料結果
	 */
	TemplateFieldMappingGottenResult update(UpdateTemplateFieldMappingCommand command);

	/**
	 * 依據主鍵識別碼查詢欄位對映
	 *
	 * @param id 主鍵識別碼
	 * @return 符合條件之對映資料結果 Optional
	 */
	Optional<TemplateFieldMappingGottenResult> findById(Long id);

	/**
	 * 依據主鍵識別碼刪除欄位對映
	 *
	 * @param id 主鍵識別碼
	 */
	void deleteById(Long id);

	/**
	 * 檢查指定識別碼之欄位對映是否存在
	 *
	 * @param id 主鍵識別碼
	 * @return 若存在回傳 {@code true}，否則回傳 {@code false}
	 */
	boolean existsById(Long id);

	/**
	 * 依條件動態搜尋欄位對映清單
	 *
	 * @param query 搜尋條件查詢
	 * @return 符合條件之欄位對映清單
	 */
	List<TemplateFieldMappingGottenResult> search(SearchTemplateFieldMappingQuery query);

	/**
	 * 檢查指定範本、工作表與欄位代碼之對映是否存在
	 *
	 * @param templateName       範本名稱
	 * @param templateSheetName 工作表代碼
	 * @param fieldCode          屬性代碼
	 * @return 若存在回傳 {@code true}，否則回傳 {@code false}
	 */
	boolean existsByTemplateNameAndTemplateSheetNameAndFieldCode(
			String templateName, String templateSheetName, String fieldCode);

	/**
	 * 檢查指定範本、工作表與欄位代碼之對映是否存在 (排除指定識別碼)
	 *
	 * @param templateName       範本名稱
	 * @param templateSheetName 工作表代碼
	 * @param fieldCode          屬性代碼
	 * @param id                 排除之主鍵識別碼
	 * @return 若存在回傳 {@code true}，否則回傳 {@code false}
	 */
	boolean existsByTemplateNameAndTemplateSheetNameAndFieldCodeAndIdNot(
			String templateName, String templateSheetName, String fieldCode, Long id);
}
