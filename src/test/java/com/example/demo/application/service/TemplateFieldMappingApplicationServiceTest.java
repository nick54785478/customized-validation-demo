package com.example.demo.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.application.dto.TemplateFieldMappingGottenResult;
import com.example.demo.application.dto.TemplateFieldMappingSearchedResult;
import com.example.demo.application.exception.ValidationException;
import com.example.demo.application.port.out.TemplateFieldMappingRepositoryPort;
import com.example.demo.application.shared.command.CreateTemplateFieldMappingCommand;
import com.example.demo.application.shared.command.DeleteTemplateFieldMappingCommand;
import com.example.demo.application.shared.command.UpdateTemplateFieldMappingCommand;
import com.example.demo.application.shared.query.GetTemplateFieldMappingQuery;
import com.example.demo.application.shared.query.SearchTemplateFieldMappingQuery;
import com.example.demo.application.shared.vo.YesNo;

/**
 * 範本欄位對映應用服務單元測試 (Template Field Mapping Application Service Test)
 */
@ExtendWith(MockitoExtension.class)
class TemplateFieldMappingApplicationServiceTest {

	@Mock
	private TemplateFieldMappingRepositoryPort repositoryPort;

	private TemplateFieldMappingApplicationService service;

	@BeforeEach
	void setUp() {
		service = new TemplateFieldMappingApplicationService(repositoryPort);
	}

	/**
	 * Feature: 範本欄位對映管理
	 * Scenario: 建立欄位對映成功
	 * Given: 傳入合法之建立欄位對映指令 (範本 USER_PROFILE, 工作表 USER, 屬性代碼 NAME, 標題 姓名)
	 * When: 調用 create 使用案例
	 * Then: 成功委派 RepositoryPort 儲存並回傳正確之 GottenResult
	 */
	@Test
	void create_shouldSucceed_whenCommandIsValid() {
		CreateTemplateFieldMappingCommand command = new CreateTemplateFieldMappingCommand(
				"USER_PROFILE", "USER", "NAME", "姓名", 1, YesNo.Y, "STRING", YesNo.Y);

		TemplateFieldMappingGottenResult mockResult = new TemplateFieldMappingGottenResult(
				1L, "USER_PROFILE", "USER", "NAME", "姓名", 1, YesNo.Y, "STRING", YesNo.Y,
				LocalDateTime.now(), LocalDateTime.now());

		when(repositoryPort.save(any(CreateTemplateFieldMappingCommand.class))).thenReturn(mockResult);

		TemplateFieldMappingGottenResult result = service.create(command);

		assertNotNull(result);
		assertEquals(1L, result.id());
		assertEquals("NAME", result.fieldCode());
		assertEquals("姓名", result.displayName());
		verify(repositoryPort).save(command);
	}

	/**
	 * Feature: 範本欄位對映管理
	 * Scenario: 建立欄位對映時必要欄位缺漏應拋出例外
	 * Given: 傳入欄位代碼 (fieldCode) 為空之建立指令
	 * When: 調用 create 使用案例
	 * Then: 應拋出 ValidationException 且錯誤代碼為 PARAM_INVALID
	 */
	@Test
	void create_shouldThrowValidationException_whenRequiredFieldIsBlank() {
		CreateTemplateFieldMappingCommand command = new CreateTemplateFieldMappingCommand(
				"USER_PROFILE", "USER", "", "姓名", 1, YesNo.Y, "STRING", YesNo.Y);

		ValidationException exception = assertThrows(ValidationException.class, () -> service.create(command));
		assertEquals("PARAM_INVALID", exception.getCode());
	}

	/**
	 * Feature: 範本欄位對映管理
	 * Scenario: 更新欄位對映成功
	 * Given: 資料庫存在指定識別碼記錄，傳入合法之更新指令
	 * When: 調用 update 使用案例
	 * Then: 成功委派 RepositoryPort 更新並回傳最新之 GottenResult
	 */
	@Test
	void update_shouldSucceed_whenEntityExistsAndCommandIsValid() {
		UpdateTemplateFieldMappingCommand command = new UpdateTemplateFieldMappingCommand(
				1L, "USER_PROFILE", "USER", "NAME", "使用者全名", 1, YesNo.Y, "STRING", YesNo.Y);

		TemplateFieldMappingGottenResult mockResult = new TemplateFieldMappingGottenResult(
				1L, "USER_PROFILE", "USER", "NAME", "使用者全名", 1, YesNo.Y, "STRING", YesNo.Y,
				LocalDateTime.now(), LocalDateTime.now());

		when(repositoryPort.existsById(1L)).thenReturn(true);
		when(repositoryPort.update(any(UpdateTemplateFieldMappingCommand.class))).thenReturn(mockResult);

		TemplateFieldMappingGottenResult result = service.update(command);

		assertNotNull(result);
		assertEquals("使用者全名", result.displayName());
		verify(repositoryPort).update(command);
	}

	/**
	 * Feature: 範本欄位對映管理
	 * Scenario: 更新不存在之欄位對映時拋出例外
	 * Given: 欲更新之識別碼於資料庫不存在
	 * When: 調用 update 使用案例
	 * Then: 應拋出 ValidationException 且錯誤代碼為 NOT_FOUND
	 */
	@Test
	void update_shouldThrowValidationException_whenEntityDoesNotExist() {
		UpdateTemplateFieldMappingCommand command = new UpdateTemplateFieldMappingCommand(
				999L, "USER_PROFILE", "USER", "NAME", "姓名", 1, YesNo.Y, "STRING", YesNo.Y);

		when(repositoryPort.existsById(999L)).thenReturn(false);

		ValidationException exception = assertThrows(ValidationException.class, () -> service.update(command));
		assertEquals("NOT_FOUND", exception.getCode());
	}

	/**
	 * Feature: 範本欄位對映管理
	 * Scenario: 刪除欄位對映成功
	 * Given: 資料庫存在指定識別碼之欄位對映記錄
	 * When: 調用 delete 使用案例
	 * Then: 成功委派 RepositoryPort 執行刪除操作
	 */
	@Test
	void delete_shouldSucceed_whenEntityExists() {
		DeleteTemplateFieldMappingCommand command = new DeleteTemplateFieldMappingCommand(1L);

		when(repositoryPort.existsById(1L)).thenReturn(true);

		service.delete(command);

		verify(repositoryPort).deleteById(1L);
	}

	/**
	 * Feature: 範本欄位對映管理
	 * Scenario: 刪除不存在之欄位對映時拋出例外
	 * Given: 指定刪除之識別碼於資料庫不存在
	 * When: 調用 delete 使用案例
	 * Then: 應拋出 ValidationException 且錯誤代碼為 NOT_FOUND
	 */
	@Test
	void delete_shouldThrowValidationException_whenEntityDoesNotExist() {
		DeleteTemplateFieldMappingCommand command = new DeleteTemplateFieldMappingCommand(999L);

		when(repositoryPort.existsById(999L)).thenReturn(false);

		ValidationException exception = assertThrows(ValidationException.class, () -> service.delete(command));
		assertEquals("NOT_FOUND", exception.getCode());
	}

	/**
	 * Feature: 範本欄位對映管理
	 * Scenario: 單筆查詢欄位對映成功
	 * Given: 資料庫存在指定識別碼記錄
	 * When: 調用 get 使用案例
	 * Then: 成功回傳對應之 GottenResult
	 */
	@Test
	void get_shouldSucceed_whenEntityExists() {
		GetTemplateFieldMappingQuery query = new GetTemplateFieldMappingQuery(1L);
		TemplateFieldMappingGottenResult mockResult = new TemplateFieldMappingGottenResult(
				1L, "USER_PROFILE", "USER", "NAME", "姓名", 1, YesNo.Y, "STRING", YesNo.Y,
				LocalDateTime.now(), LocalDateTime.now());

		when(repositoryPort.findById(1L)).thenReturn(Optional.of(mockResult));

		TemplateFieldMappingGottenResult result = service.get(query);

		assertNotNull(result);
		assertEquals(1L, result.id());
		assertEquals("NAME", result.fieldCode());
	}

	/**
	 * Feature: 範本欄位對映管理
	 * Scenario: 查詢不存在之欄位對映時拋出例外
	 * Given: 指定識別碼不存在
	 * When: 調用 get 使用案例
	 * Then: 應拋出 ValidationException 且錯誤代碼為 NOT_FOUND
	 */
	@Test
	void get_shouldThrowValidationException_whenEntityDoesNotExist() {
		GetTemplateFieldMappingQuery query = new GetTemplateFieldMappingQuery(999L);

		when(repositoryPort.findById(999L)).thenReturn(Optional.empty());

		ValidationException exception = assertThrows(ValidationException.class, () -> service.get(query));
		assertEquals("NOT_FOUND", exception.getCode());
	}

	/**
	 * Feature: 範本欄位對映管理
	 * Scenario: 依條件動態搜尋欄位對映清單
	 * Given: 傳入範本名稱與工作表名稱之查詢條件
	 * When: 調用 search 使用案例
	 * Then: 回傳符合條件之清單與正確筆數
	 */
	@Test
	void search_shouldReturnResults_whenMatchingCriteria() {
		SearchTemplateFieldMappingQuery query = new SearchTemplateFieldMappingQuery(
				"USER_PROFILE", "USER", null, null, YesNo.Y);

		List<TemplateFieldMappingGottenResult> mockList = List.of(
				new TemplateFieldMappingGottenResult(1L, "USER_PROFILE", "USER", "NAME", "姓名", 1, YesNo.Y, "STRING", YesNo.Y, LocalDateTime.now(), LocalDateTime.now()),
				new TemplateFieldMappingGottenResult(2L, "USER_PROFILE", "USER", "AGE", "年齡", 2, YesNo.Y, "NUMBER", YesNo.Y, LocalDateTime.now(), LocalDateTime.now()));

		when(repositoryPort.search(query)).thenReturn(mockList);

		TemplateFieldMappingSearchedResult result = service.search(query);

		assertNotNull(result);
		assertEquals(2, result.total());
		assertEquals(2, result.items().size());
		assertEquals("NAME", result.items().get(0).fieldCode());
	}

	/**
	 * Feature: 範本欄位對映管理
	 * Scenario: 建立欄位對映時重複鍵檢核失敗
	 * Given: 資料庫已存在相同 templateName, templateSheetName 與 fieldCode 之對映
	 * When: 調用 create 使用案例
	 * Then: 應拋出 ValidationException 且錯誤代碼為 DUPLICATE_KEY
	 */
	@Test
	void create_shouldThrowValidationException_whenDuplicateKeyExists() {
		CreateTemplateFieldMappingCommand command = new CreateTemplateFieldMappingCommand(
				"USER_PROFILE", "USER", "NAME", "姓名", 1, YesNo.Y, "STRING", YesNo.Y);

		when(repositoryPort.existsByTemplateNameAndTemplateSheetNameAndFieldCode("USER_PROFILE", "USER", "NAME")).thenReturn(true);

		ValidationException exception = assertThrows(ValidationException.class, () -> service.create(command));
		assertEquals("DUPLICATE_KEY", exception.getCode());
	}

	/**
	 * Feature: 範本欄位對映管理
	 * Scenario: 更新欄位對映時重複鍵檢核失敗
	 * Given: 更新之 templateName, templateSheetName 與 fieldCode 已被其他記錄使用
	 * When: 調用 update 使用案例
	 * Then: 應拋出 ValidationException 且錯誤代碼為 DUPLICATE_KEY
	 */
	@Test
	void update_shouldThrowValidationException_whenDuplicateKeyExists() {
		UpdateTemplateFieldMappingCommand command = new UpdateTemplateFieldMappingCommand(
				1L, "USER_PROFILE", "USER", "AGE", "年齡", 2, YesNo.Y, "NUMBER", YesNo.Y);

		when(repositoryPort.existsById(1L)).thenReturn(true);
		when(repositoryPort.existsByTemplateNameAndTemplateSheetNameAndFieldCodeAndIdNot("USER_PROFILE", "USER", "AGE", 1L))
				.thenReturn(true);

		ValidationException exception = assertThrows(ValidationException.class, () -> service.update(command));
		assertEquals("DUPLICATE_KEY", exception.getCode());
	}

	/**
	 * Feature: 範本欄位對映管理
	 * Scenario: 操作無效之識別碼 (id <= 0)
	 * Given: 傳入 ID 為 0 或負數
	 * When: 調用 get / update / delete 使用案例
	 * Then: 應拋出 ValidationException 且錯誤代碼為 PARAM_INVALID
	 */
	@Test
	void shouldThrowValidationException_whenIdIsInvalid() {
		GetTemplateFieldMappingQuery getQuery = new GetTemplateFieldMappingQuery(0L);
		ValidationException getEx = assertThrows(ValidationException.class, () -> service.get(getQuery));
		assertEquals("PARAM_INVALID", getEx.getCode());

		UpdateTemplateFieldMappingCommand updateCmd = new UpdateTemplateFieldMappingCommand(
				-1L, "USER_PROFILE", "USER", "NAME", "姓名", 1, YesNo.Y, "STRING", YesNo.Y);
		ValidationException updateEx = assertThrows(ValidationException.class, () -> service.update(updateCmd));
		assertEquals("PARAM_INVALID", updateEx.getCode());

		DeleteTemplateFieldMappingCommand deleteCmd = new DeleteTemplateFieldMappingCommand(0L);
		ValidationException deleteEx = assertThrows(ValidationException.class, () -> service.delete(deleteCmd));
		assertEquals("PARAM_INVALID", deleteEx.getCode());
	}
}
