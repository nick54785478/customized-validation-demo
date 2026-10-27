package com.example.demo.iface.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.application.service.UploadCommandService;
import com.example.demo.config.WebConfig;
import com.example.demo.iface.assembler.UploadResourceAssembler;

/**
 * 範本上傳控制器整合測試 (Upload Controller Integration Test)
 */
@WebMvcTest(UploadController.class)
@Import({UploadResourceAssembler.class, WebConfig.class})
class UploadControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UploadCommandService uploadCommandService;

	/**
	 * Feature: 範本上傳 REST API
	 * Scenario: 上傳合法檔案與參數
	 * Given: 傳入合法之 resource JSON 與非空之 Excel 檔案
	 * When: 發送 POST /api/v1/upload (multipart/form-data)
	 * Then: 回傳 HTTP 200 OK 與成功提示
	 */
	@Test
	void upload_shouldReturn200_whenValid() throws Exception {
		doNothing().when(uploadCommandService).upload(any(), any());

		MockMultipartFile resourcePart = new MockMultipartFile(
				"resource",
				"",
				MediaType.APPLICATION_JSON_VALUE,
				"""
				{
				  "name": "USER_PROFILE",
				  "type": "EXCEL",
				  "fileType": "XLSX"
				}
				""".getBytes());

		MockMultipartFile filePart = new MockMultipartFile(
				"file",
				"test.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				new byte[]{1, 2, 3});

		mockMvc.perform(multipart("/api/v1/upload")
				.file(resourcePart)
				.file(filePart))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("200"))
				.andExpect(jsonPath("$.message").value("上傳成功"));
	}

	/**
	 * Feature: 範本上傳 REST API 參數檢核
	 * Scenario: 上傳請求中 resource 參數不合法
	 * Given: resource 中 name 為空
	 * When: 發送 POST /api/v1/upload
	 * Then: 回傳 HTTP 400 Bad Request 與 PARAM_VALIDATION_FAILED 代碼
	 */
	@Test
	void upload_shouldReturn400_whenResourceIsInvalid() throws Exception {
		MockMultipartFile resourcePart = new MockMultipartFile(
				"resource",
				"",
				MediaType.APPLICATION_JSON_VALUE,
				"""
				{
				  "name": "",
				  "type": ""
				}
				""".getBytes());

		MockMultipartFile filePart = new MockMultipartFile(
				"file",
				"test.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				new byte[]{1, 2, 3});

		mockMvc.perform(multipart("/api/v1/upload")
				.file(resourcePart)
				.file(filePart))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PARAM_VALIDATION_FAILED"))
				.andExpect(jsonPath("$.messages").isArray());
	}

	/**
	 * Feature: 範本上傳 REST API 參數檢核
	 * Scenario: 上傳檔案為空
	 * Given: file 長度為 0
	 * When: 發送 POST /api/v1/upload
	 * Then: 回傳 HTTP 400 Bad Request 與 PARAM_INVALID 代碼
	 */
	@Test
	void upload_shouldReturn400_whenFileIsEmpty() throws Exception {
		MockMultipartFile resourcePart = new MockMultipartFile(
				"resource",
				"",
				MediaType.APPLICATION_JSON_VALUE,
				"""
				{
				  "name": "USER_PROFILE",
				  "type": "EXCEL"
				}
				""".getBytes());

		MockMultipartFile emptyFilePart = new MockMultipartFile(
				"file",
				"empty.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				new byte[0]);

		mockMvc.perform(multipart("/api/v1/upload")
				.file(resourcePart)
				.file(emptyFilePart))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PARAM_INVALID"));
	}

	/**
	 * Feature: 範本上傳 REST API (Swagger UI 媒體型別相容性)
	 * Scenario: 當客戶端或 Swagger UI 以 application/octet-stream 發送 resource 參數時仍能正常解析
	 * Given: resource 內容為 JSON 但 Content-Type 為 application/octet-stream
	 * When: 發送 POST /api/v1/upload
	 * Then: WebConfig 應使 Jackson 正常反序列化並回傳 HTTP 200 OK
	 */
	@Test
	void upload_shouldReturn200_whenResourceIsOctetStream() throws Exception {
		doNothing().when(uploadCommandService).upload(any(), any());

		MockMultipartFile resourcePart = new MockMultipartFile(
				"resource",
				"",
				MediaType.APPLICATION_OCTET_STREAM_VALUE,
				"""
				{
				  "name": "PR_IMPORT",
				  "type": "PR",
				  "fileType": "XLSX"
				}
				""".getBytes());

		MockMultipartFile filePart = new MockMultipartFile(
				"file",
				"PR_IMPORT.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				new byte[]{1, 2, 3});

		mockMvc.perform(multipart("/api/v1/upload")
				.file(resourcePart)
				.file(filePart))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("200"))
				.andExpect(jsonPath("$.message").value("上傳成功"));
	}
}
