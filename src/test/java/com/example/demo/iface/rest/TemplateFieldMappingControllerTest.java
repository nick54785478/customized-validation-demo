package com.example.demo.iface.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.application.dto.TemplateFieldMappingGottenResult;
import com.example.demo.application.dto.TemplateFieldMappingSearchedResult;
import com.example.demo.application.exception.ValidationException;
import com.example.demo.application.port.in.CreateTemplateFieldMappingUseCase;
import com.example.demo.application.port.in.DeleteTemplateFieldMappingUseCase;
import com.example.demo.application.port.in.GetTemplateFieldMappingUseCase;
import com.example.demo.application.port.in.SearchTemplateFieldMappingUseCase;
import com.example.demo.application.port.in.UpdateTemplateFieldMappingUseCase;
import com.example.demo.application.shared.command.CreateTemplateFieldMappingCommand;
import com.example.demo.application.shared.command.UpdateTemplateFieldMappingCommand;
import com.example.demo.application.shared.query.GetTemplateFieldMappingQuery;
import com.example.demo.application.shared.query.SearchTemplateFieldMappingQuery;
import com.example.demo.application.shared.vo.YesNo;
import com.example.demo.iface.assembler.TemplateFieldMappingResourceAssembler;

/**
 * 範本欄位對映控制器整合測試 (Template Field Mapping Controller Integration Test)
 */
@WebMvcTest(TemplateFieldMappingController.class)
@Import(TemplateFieldMappingResourceAssembler.class)
class TemplateFieldMappingControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CreateTemplateFieldMappingUseCase createTemplateFieldMappingUseCase;

	@MockitoBean
	private UpdateTemplateFieldMappingUseCase updateTemplateFieldMappingUseCase;

	@MockitoBean
	private DeleteTemplateFieldMappingUseCase deleteTemplateFieldMappingUseCase;

	@MockitoBean
	private GetTemplateFieldMappingUseCase getTemplateFieldMappingUseCase;

	@MockitoBean
	private SearchTemplateFieldMappingUseCase searchTemplateFieldMappingUseCase;

	/**
	 * Feature: 欄位對映 REST API
	 * Scenario: 透過 POST 新增欄位對映
	 * Given: 傳入合法 JSON 請求資源
	 * When: 發送 POST /api/v1/template-field-mappings
	 * Then: 回傳 HTTP 201 Created 與建立結果
	 */
	@Test
	void create_shouldReturn201() throws Exception {
		TemplateFieldMappingGottenResult mockResult = new TemplateFieldMappingGottenResult(
				1L, "USER_PROFILE", "USER", "NAME", "姓名", 1, YesNo.Y, "STRING", YesNo.Y,
				LocalDateTime.now(), LocalDateTime.now());

		when(createTemplateFieldMappingUseCase.create(any(CreateTemplateFieldMappingCommand.class)))
				.thenReturn(mockResult);

		String json = """
				{
				  "templateName": "USER_PROFILE",
				  "templateSheetName": "USER",
				  "fieldCode": "NAME",
				  "displayName": "姓名",
				  "columnIndex": 1,
				  "isRequired": "Y",
				  "dataType": "STRING",
				  "activeFlag": "Y"
				}
				""";

		mockMvc.perform(post("/api/v1/template-field-mappings")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.fieldCode").value("NAME"))
				.andExpect(jsonPath("$.displayName").value("姓名"));
	}

	/**
	 * Feature: 欄位對映 REST API
	 * Scenario: 透過 GET 查詢單一欄位對映
	 * Given: 存在 ID 為 1 的記錄
	 * When: 發送 GET /api/v1/template-field-mappings/1
	 * Then: 回傳 HTTP 200 OK 與對應資料
	 */
	@Test
	void get_shouldReturn200() throws Exception {
		TemplateFieldMappingGottenResult mockResult = new TemplateFieldMappingGottenResult(
				1L, "USER_PROFILE", "USER", "NAME", "姓名", 1, YesNo.Y, "STRING", YesNo.Y,
				LocalDateTime.now(), LocalDateTime.now());

		when(getTemplateFieldMappingUseCase.get(any(GetTemplateFieldMappingQuery.class)))
				.thenReturn(mockResult);

		mockMvc.perform(get("/api/v1/template-field-mappings/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.fieldCode").value("NAME"));
	}

	/**
	 * Feature: 欄位對映 REST API
	 * Scenario: 查詢不存在之欄位對映
	 * Given: 指定 ID 記錄不存在
	 * When: 發送 GET /api/v1/template-field-mappings/999
	 * Then: 回傳 HTTP 404 NOT_FOUND
	 */
	@Test
	void get_shouldReturn404_whenNotFound() throws Exception {
		when(getTemplateFieldMappingUseCase.get(any(GetTemplateFieldMappingQuery.class)))
				.thenThrow(new ValidationException("NOT_FOUND", "找不到指定之欄位對映資料: id=999"));

		mockMvc.perform(get("/api/v1/template-field-mappings/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	/**
	 * Feature: 欄位對映 REST API
	 * Scenario: 透過 PUT 更新欄位對映
	 * Given: 傳入合法 JSON 更新資源
	 * When: 發送 PUT /api/v1/template-field-mappings/1
	 * Then: 回傳 HTTP 200 OK 與更新結果
	 */
	@Test
	void update_shouldReturn200() throws Exception {
		TemplateFieldMappingGottenResult mockResult = new TemplateFieldMappingGottenResult(
				1L, "USER_PROFILE", "USER", "NAME", "全名", 1, YesNo.Y, "STRING", YesNo.Y,
				LocalDateTime.now(), LocalDateTime.now());

		when(updateTemplateFieldMappingUseCase.update(any(UpdateTemplateFieldMappingCommand.class)))
				.thenReturn(mockResult);

		String json = """
				{
				  "templateName": "USER_PROFILE",
				  "templateSheetName": "USER",
				  "fieldCode": "NAME",
				  "displayName": "全名",
				  "columnIndex": 1,
				  "isRequired": "Y",
				  "dataType": "STRING",
				  "activeFlag": "Y"
				}
				""";

		mockMvc.perform(put("/api/v1/template-field-mappings/1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").value("全名"));
	}

	/**
	 * Feature: 欄位對映 REST API
	 * Scenario: 透過 DELETE 刪除欄位對映
	 * Given: 指定 ID 為 1
	 * When: 發送 DELETE /api/v1/template-field-mappings/1
	 * Then: 回傳 HTTP 200 OK 與成功提示
	 */
	@Test
	void delete_shouldReturn200() throws Exception {
		mockMvc.perform(delete("/api/v1/template-field-mappings/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	/**
	 * Feature: 欄位對映 REST API
	 * Scenario: 透過 GET 條件搜尋欄位對映清單
	 * Given: 傳入 templateName=USER_PROFILE 與 templateSheetName=USER
	 * When: 發送 GET /api/v1/template-field-mappings?templateName=USER_PROFILE&templateSheetName=USER
	 * Then: 回傳 HTTP 200 OK 與清單物件
	 */
	@Test
	void search_shouldReturn200() throws Exception {
		TemplateFieldMappingSearchedResult mockResult = new TemplateFieldMappingSearchedResult(
				List.of(new TemplateFieldMappingGottenResult(
						1L, "USER_PROFILE", "USER", "NAME", "姓名", 1, YesNo.Y, "STRING", YesNo.Y,
						LocalDateTime.now(), LocalDateTime.now())), 1);

		when(searchTemplateFieldMappingUseCase.search(any(SearchTemplateFieldMappingQuery.class)))
				.thenReturn(mockResult);

		mockMvc.perform(get("/api/v1/template-field-mappings")
				.param("templateName", "USER_PROFILE")
				.param("templateSheetName", "USER"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(1))
				.andExpect(jsonPath("$.items[0].fieldCode").value("NAME"));
	}

	/**
	 * Feature: 欄位對映 REST API 參數檢核
	 * Scenario: 透過 POST 新增欄位對映時傳入非法參數
	 * Given: 傳入欄位為空之無效請求
	 * When: 發送 POST /api/v1/template-field-mappings
	 * Then: 攔截並回傳 HTTP 400 Bad Request 與 PARAM_VALIDATION_FAILED 代碼
	 */
	@Test
	void create_shouldReturn400_whenPayloadIsInvalid() throws Exception {
		String invalidJson = """
				{
				  "templateName": "",
				  "templateSheetName": "",
				  "fieldCode": "",
				  "displayName": ""
				}
				""";

		mockMvc.perform(post("/api/v1/template-field-mappings")
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidJson))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PARAM_VALIDATION_FAILED"))
				.andExpect(jsonPath("$.messages").isArray());
	}
}
