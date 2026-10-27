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

import com.example.demo.application.dto.TemplateSheetMappingGottenResult;
import com.example.demo.application.dto.TemplateSheetMappingSearchedResult;
import com.example.demo.application.exception.ValidationException;
import com.example.demo.application.port.in.CreateTemplateSheetMappingUseCase;
import com.example.demo.application.port.in.DeleteTemplateSheetMappingUseCase;
import com.example.demo.application.port.in.GetTemplateSheetMappingUseCase;
import com.example.demo.application.port.in.SearchTemplateSheetMappingUseCase;
import com.example.demo.application.port.in.UpdateTemplateSheetMappingUseCase;
import com.example.demo.application.shared.command.CreateTemplateSheetMappingCommand;
import com.example.demo.application.shared.command.UpdateTemplateSheetMappingCommand;
import com.example.demo.application.shared.query.GetTemplateSheetMappingQuery;
import com.example.demo.application.shared.query.SearchTemplateSheetMappingQuery;
import com.example.demo.application.shared.vo.YesNo;
import com.example.demo.iface.assembler.TemplateSheetMappingResourceAssembler;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 範本工作表對映控制器整合測試 (Template Sheet Mapping Controller Integration Test)
 */
@WebMvcTest(TemplateSheetMappingController.class)
@Import(TemplateSheetMappingResourceAssembler.class)
class TemplateSheetMappingControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private CreateTemplateSheetMappingUseCase createTemplateSheetMappingUseCase;

	@MockitoBean
	private UpdateTemplateSheetMappingUseCase updateTemplateSheetMappingUseCase;

	@MockitoBean
	private DeleteTemplateSheetMappingUseCase deleteTemplateSheetMappingUseCase;

	@MockitoBean
	private GetTemplateSheetMappingUseCase getTemplateSheetMappingUseCase;

	@MockitoBean
	private SearchTemplateSheetMappingUseCase searchTemplateSheetMappingUseCase;

	/**
	 * Feature: 工作表對映 REST API
	 * Scenario: 透過 POST 新增工作表對映
	 * Given: 傳入合法 JSON 請求資源
	 * When: 發送 POST /api/v1/template-sheet-mappings
	 * Then: 回傳 HTTP 201 Created 與建立結果
	 */
	@Test
	void create_shouldReturn201() throws Exception {
		TemplateSheetMappingGottenResult mockResult = new TemplateSheetMappingGottenResult(
				1L, "USER_PROFILE", "USER", "使用者名單", 0, YesNo.Y, YesNo.Y,
				LocalDateTime.now(), LocalDateTime.now());

		when(createTemplateSheetMappingUseCase.create(any(CreateTemplateSheetMappingCommand.class)))
				.thenReturn(mockResult);

		String json = """
				{
				  "templateName": "USER_PROFILE",
				  "sheetCode": "USER",
				  "displayName": "使用者名單",
				  "sheetIndex": 0,
				  "isRequired": "Y",
				  "activeFlag": "Y"
				}
				""";

		mockMvc.perform(post("/api/v1/template-sheet-mappings")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.sheetCode").value("USER"))
				.andExpect(jsonPath("$.displayName").value("使用者名單"));
	}

	/**
	 * Feature: 工作表對映 REST API
	 * Scenario: 透過 GET 查詢單一工作表對映
	 * Given: 存在 ID 為 1 的記錄
	 * When: 發送 GET /api/v1/template-sheet-mappings/1
	 * Then: 回傳 HTTP 200 OK 與對應資料
	 */
	@Test
	void get_shouldReturn200() throws Exception {
		TemplateSheetMappingGottenResult mockResult = new TemplateSheetMappingGottenResult(
				1L, "USER_PROFILE", "USER", "使用者名單", 0, YesNo.Y, YesNo.Y,
				LocalDateTime.now(), LocalDateTime.now());

		when(getTemplateSheetMappingUseCase.get(any(GetTemplateSheetMappingQuery.class)))
				.thenReturn(mockResult);

		mockMvc.perform(get("/api/v1/template-sheet-mappings/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.sheetCode").value("USER"));
	}

	/**
	 * Feature: 工作表對映 REST API
	 * Scenario: 查詢不存在之工作表對映
	 * Given: 指定 ID 記錄不存在
	 * When: 發送 GET /api/v1/template-sheet-mappings/999
	 * Then: 回傳 HTTP 404 NOT_FOUND
	 */
	@Test
	void get_shouldReturn404_whenNotFound() throws Exception {
		when(getTemplateSheetMappingUseCase.get(any(GetTemplateSheetMappingQuery.class)))
				.thenThrow(new ValidationException("NOT_FOUND", "找不到指定之工作表對映資料: id=999"));

		mockMvc.perform(get("/api/v1/template-sheet-mappings/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	/**
	 * Feature: 工作表對映 REST API
	 * Scenario: 透過 PUT 更新工作表對映
	 * Given: 傳入合法 JSON 更新資源
	 * When: 發送 PUT /api/v1/template-sheet-mappings/1
	 * Then: 回傳 HTTP 200 OK 與更新結果
	 */
	@Test
	void update_shouldReturn200() throws Exception {
		TemplateSheetMappingGottenResult mockResult = new TemplateSheetMappingGottenResult(
				1L, "USER_PROFILE", "USER", "使用者名冊", 0, YesNo.Y, YesNo.Y,
				LocalDateTime.now(), LocalDateTime.now());

		when(updateTemplateSheetMappingUseCase.update(any(UpdateTemplateSheetMappingCommand.class)))
				.thenReturn(mockResult);

		String json = """
				{
				  "templateName": "USER_PROFILE",
				  "sheetCode": "USER",
				  "displayName": "使用者名冊",
				  "sheetIndex": 0,
				  "isRequired": "Y",
				  "activeFlag": "Y"
				}
				""";

		mockMvc.perform(put("/api/v1/template-sheet-mappings/1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").value("使用者名冊"));
	}

	/**
	 * Feature: 工作表對映 REST API
	 * Scenario: 透過 DELETE 刪除工作表對映
	 * Given: 指定 ID 為 1
	 * When: 發送 DELETE /api/v1/template-sheet-mappings/1
	 * Then: 回傳 HTTP 200 OK 與成功提示
	 */
	@Test
	void delete_shouldReturn200() throws Exception {
		mockMvc.perform(delete("/api/v1/template-sheet-mappings/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	/**
	 * Feature: 工作表對映 REST API
	 * Scenario: 透過 GET 條件搜尋工作表對映清單
	 * Given: 傳入 templateName=USER_PROFILE
	 * When: 發送 GET /api/v1/template-sheet-mappings?templateName=USER_PROFILE
	 * Then: 回傳 HTTP 200 OK 與清單物件
	 */
	@Test
	void search_shouldReturn200() throws Exception {
		TemplateSheetMappingSearchedResult mockResult = new TemplateSheetMappingSearchedResult(
				List.of(new TemplateSheetMappingGottenResult(
						1L, "USER_PROFILE", "USER", "使用者名單", 0, YesNo.Y, YesNo.Y,
						LocalDateTime.now(), LocalDateTime.now())), 1);

		when(searchTemplateSheetMappingUseCase.search(any(SearchTemplateSheetMappingQuery.class)))
				.thenReturn(mockResult);

		mockMvc.perform(get("/api/v1/template-sheet-mappings")
				.param("templateName", "USER_PROFILE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(1))
				.andExpect(jsonPath("$.items[0].sheetCode").value("USER"));
	}

	/**
	 * Feature: 工作表對映 REST API 參數檢核
	 * Scenario: 透過 POST 新增工作表對映時傳入非法參數
	 * Given: 傳入欄位為空之無效請求
	 * When: 發送 POST /api/v1/template-sheet-mappings
	 * Then: 攔截並回傳 HTTP 400 Bad Request 與 PARAM_VALIDATION_FAILED 代碼
	 */
	@Test
	void create_shouldReturn400_whenPayloadIsInvalid() throws Exception {
		String invalidJson = """
				{
				  "templateName": "",
				  "sheetCode": "",
				  "displayName": ""
				}
				""";

		mockMvc.perform(post("/api/v1/template-sheet-mappings")
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidJson))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PARAM_VALIDATION_FAILED"))
				.andExpect(jsonPath("$.messages").isArray());
	}
}
