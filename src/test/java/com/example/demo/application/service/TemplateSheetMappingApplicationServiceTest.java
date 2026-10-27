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

import com.example.demo.application.dto.TemplateSheetMappingGottenResult;
import com.example.demo.application.dto.TemplateSheetMappingSearchedResult;
import com.example.demo.application.exception.ValidationException;
import com.example.demo.application.port.out.TemplateSheetMappingRepositoryPort;
import com.example.demo.application.shared.command.CreateTemplateSheetMappingCommand;
import com.example.demo.application.shared.command.DeleteTemplateSheetMappingCommand;
import com.example.demo.application.shared.command.UpdateTemplateSheetMappingCommand;
import com.example.demo.application.shared.query.GetTemplateSheetMappingQuery;
import com.example.demo.application.shared.query.SearchTemplateSheetMappingQuery;
import com.example.demo.application.shared.vo.YesNo;

/**
 * 範本工作表對映應用服務單元測試 (Template Sheet Mapping Application Service Test)
 */
@ExtendWith(MockitoExtension.class)
class TemplateSheetMappingApplicationServiceTest {

	@Mock
	private TemplateSheetMappingRepositoryPort repositoryPort;

	private TemplateSheetMappingApplicationService service;

	@BeforeEach
	void setUp() {
		service = new TemplateSheetMappingApplicationService(repositoryPort);
	}

	/**
	 * Feature: 範本工作表對映管理
	 * Scenario: 建立工作表對映成功
	 * Given: 傳入合法之建立工作表對映指令 (範本 USER_PROFILE, 工作表代碼 USER, 顯示名稱 使用者名單)
	 * When: 調用 create 使用案例
	 * Then: 成功委派 RepositoryPort 儲存並回傳正確之 GottenResult
	 */
	@Test
	void create_shouldSucceed_whenCommandIsValid() {
		CreateTemplateSheetMappingCommand command = new CreateTemplateSheetMappingCommand(
				"USER_PROFILE", "USER", "使用者名單", 0, YesNo.Y, YesNo.Y);

		TemplateSheetMappingGottenResult mockResult = new TemplateSheetMappingGottenResult(
				1L, "USER_PROFILE", "USER", "使用者名單", 0, YesNo.Y, YesNo.Y,
				LocalDateTime.now(), LocalDateTime.now());

		when(repositoryPort.save(any(CreateTemplateSheetMappingCommand.class))).thenReturn(mockResult);

		TemplateSheetMappingGottenResult result = service.create(command);

		assertNotNull(result);
		assertEquals(1L, result.id());
		assertEquals("USER", result.sheetCode());
		assertEquals("使用者名單", result.displayName());
		verify(repositoryPort).save(command);
	}

	/**
	 * Feature: 範本工作表對映管理
	 * Scenario: 建立工作表對映時必填欄位為空應拋出例外
	 * Given: 傳入範本名稱為空之建立指令
	 * When: 調用 create 使用案例
	 * Then: 應拋出 ValidationException 且錯誤代碼為 PARAM_INVALID
	 */
	@Test
	void create_shouldThrowValidationException_whenRequiredFieldIsBlank() {
		CreateTemplateSheetMappingCommand command = new CreateTemplateSheetMappingCommand(
				"", "USER", "使用者名單", 0, YesNo.Y, YesNo.Y);

		ValidationException exception = assertThrows(ValidationException.class, () -> service.create(command));
		assertEquals("PARAM_INVALID", exception.getCode());
	}

	/**
	 * Feature: 範本工作表對映管理
	 * Scenario: 更新工作表對映成功
	 * Given: 資料庫存在指定識別碼記錄，傳入合法之更新指令
	 * When: 調用 update 使用案例
	 * Then: 成功委派 RepositoryPort 更新並回傳最新之 GottenResult
	 */
	@Test
	void update_shouldSucceed_whenEntityExistsAndCommandIsValid() {
		UpdateTemplateSheetMappingCommand command = new UpdateTemplateSheetMappingCommand(
				1L, "USER_PROFILE", "USER", "使用者資料", 0, YesNo.Y, YesNo.Y);

		TemplateSheetMappingGottenResult mockResult = new TemplateSheetMappingGottenResult(
				1L, "USER_PROFILE", "USER", "使用者資料", 0, YesNo.Y, YesNo.Y,
				LocalDateTime.now(), LocalDateTime.now());

		when(repositoryPort.existsById(1L)).thenReturn(true);
		when(repositoryPort.update(any(UpdateTemplateSheetMappingCommand.class))).thenReturn(mockResult);

		TemplateSheetMappingGottenResult result = service.update(command);

		assertNotNull(result);
		assertEquals("使用者資料", result.displayName());
		verify(repositoryPort).update(command);
	}

	/**
	 * Feature: 範本工作表對映管理
	 * Scenario: 更新不存在之工作表對映時拋出例外
	 * Given: 欲更新之識別碼於資料庫不存在
	 * When: 調用 update 使用案例
	 * Then: 應拋出 ValidationException 且錯誤代碼為 NOT_FOUND
	 */
	@Test
	void update_shouldThrowValidationException_whenEntityDoesNotExist() {
		UpdateTemplateSheetMappingCommand command = new UpdateTemplateSheetMappingCommand(
				999L, "USER_PROFILE", "USER", "使用者資料", 0, YesNo.Y, YesNo.Y);

		when(repositoryPort.existsById(999L)).thenReturn(false);

		ValidationException exception = assertThrows(ValidationException.class, () -> service.update(command));
		assertEquals("NOT_FOUND", exception.getCode());
	}

	/**
	 * Feature: 範本工作表對映管理
	 * Scenario: 刪除工作表對映成功
	 * Given: 資料庫存在指定識別碼之工作表對映記錄
	 * When: 調用 delete 使用案例
	 * Then: 成功委派 RepositoryPort 執行刪除操作
	 */
	@Test
	void delete_shouldSucceed_whenEntityExists() {
		DeleteTemplateSheetMappingCommand command = new DeleteTemplateSheetMappingCommand(1L);

		when(repositoryPort.existsById(1L)).thenReturn(true);

		service.delete(command);

		verify(repositoryPort).deleteById(1L);
	}

	/**
	 * Feature: 範本工作表對映管理
	 * Scenario: 刪除不存在之工作表對映時拋出例外
	 * Given: 指定刪除之識別碼於資料庫不存在
	 * When: 調用 delete 使用案例
	 * Then: 應拋出 ValidationException 且錯誤代碼為 NOT_FOUND
	 */
	@Test
	void delete_shouldThrowValidationException_whenEntityDoesNotExist() {
		DeleteTemplateSheetMappingCommand command = new DeleteTemplateSheetMappingCommand(999L);

		when(repositoryPort.existsById(999L)).thenReturn(false);

		ValidationException exception = assertThrows(ValidationException.class, () -> service.delete(command));
		assertEquals("NOT_FOUND", exception.getCode());
	}

	/**
	 * Feature: 範本工作表對映管理
	 * Scenario: 單筆查詢工作表對映成功
	 * Given: 資料庫存在指定識別碼記錄
	 * When: 調用 get 使用案例
	 * Then: 成功回傳對應之 GottenResult
	 */
	@Test
	void get_shouldSucceed_whenEntityExists() {
		GetTemplateSheetMappingQuery query = new GetTemplateSheetMappingQuery(1L);
		TemplateSheetMappingGottenResult mockResult = new TemplateSheetMappingGottenResult(
				1L, "USER_PROFILE", "USER", "使用者名單", 0, YesNo.Y, YesNo.Y,
				LocalDateTime.now(), LocalDateTime.now());

		when(repositoryPort.findById(1L)).thenReturn(Optional.of(mockResult));

		TemplateSheetMappingGottenResult result = service.get(query);

		assertNotNull(result);
		assertEquals(1L, result.id());
		assertEquals("USER", result.sheetCode());
	}

	/**
	 * Feature: 範本工作表對映管理
	 * Scenario: 查詢不存在之工作表對映時拋出例外
	 * Given: 指定識別碼不存在
	 * When: 調用 get 使用案例
	 * Then: 應拋出 ValidationException 且錯誤代碼為 NOT_FOUND
	 */
	@Test
	void get_shouldThrowValidationException_whenEntityDoesNotExist() {
		GetTemplateSheetMappingQuery query = new GetTemplateSheetMappingQuery(999L);

		when(repositoryPort.findById(999L)).thenReturn(Optional.empty());

		ValidationException exception = assertThrows(ValidationException.class, () -> service.get(query));
		assertEquals("NOT_FOUND", exception.getCode());
	}

	/**
	 * Feature: 範本工作表對映管理
	 * Scenario: 依條件動態搜尋工作表對映清單
	 * Given: 傳入範本名稱為 USER_PROFILE 之查詢條件
	 * When: 調用 search 使用案例
	 * Then: 回傳符合條件之清單與正確筆數
	 */
	@Test
	void search_shouldReturnResults_whenMatchingCriteria() {
		SearchTemplateSheetMappingQuery query = new SearchTemplateSheetMappingQuery(
				"USER_PROFILE", null, null, YesNo.Y);

		List<TemplateSheetMappingGottenResult> mockList = List.of(
				new TemplateSheetMappingGottenResult(1L, "USER_PROFILE", "USER", "使用者名單", 0, YesNo.Y, YesNo.Y, LocalDateTime.now(), LocalDateTime.now()),
				new TemplateSheetMappingGottenResult(2L, "USER_PROFILE", "DEPT", "所屬部門", 1, YesNo.Y, YesNo.Y, LocalDateTime.now(), LocalDateTime.now()));

		when(repositoryPort.search(query)).thenReturn(mockList);

		TemplateSheetMappingSearchedResult result = service.search(query);

		assertNotNull(result);
		assertEquals(2, result.total());
		assertEquals(2, result.items().size());
		assertEquals("USER", result.items().get(0).sheetCode());
	}

	/**
	 * Feature: 範本工作表對映管理
	 * Scenario: 建立工作表對映時重複鍵檢核失敗
	 * Given: 資料庫已存在相同 templateName 與 sheetCode 之對映
	 * When: 調用 create 使用案例
	 * Then: 應拋出 ValidationException 且錯誤代碼為 DUPLICATE_KEY
	 */
	@Test
	void create_shouldThrowValidationException_whenDuplicateKeyExists() {
		CreateTemplateSheetMappingCommand command = new CreateTemplateSheetMappingCommand(
				"USER_PROFILE", "USER", "使用者名單", 0, YesNo.Y, YesNo.Y);

		when(repositoryPort.existsByTemplateNameAndSheetCode("USER_PROFILE", "USER")).thenReturn(true);

		ValidationException exception = assertThrows(ValidationException.class, () -> service.create(command));
		assertEquals("DUPLICATE_KEY", exception.getCode());
	}

	/**
	 * Feature: 範本工作表對映管理
	 * Scenario: 更新工作表對映時重複鍵檢核失敗
	 * Given: 更新之 templateName 與 sheetCode 已被其他記錄使用
	 * When: 調用 update 使用案例
	 * Then: 應拋出 ValidationException 且錯誤代碼為 DUPLICATE_KEY
	 */
	@Test
	void update_shouldThrowValidationException_whenDuplicateKeyExists() {
		UpdateTemplateSheetMappingCommand command = new UpdateTemplateSheetMappingCommand(
				1L, "USER_PROFILE", "DEPT", "部門資料", 1, YesNo.Y, YesNo.Y);

		when(repositoryPort.existsById(1L)).thenReturn(true);
		when(repositoryPort.existsByTemplateNameAndSheetCodeAndIdNot("USER_PROFILE", "DEPT", 1L)).thenReturn(true);

		ValidationException exception = assertThrows(ValidationException.class, () -> service.update(command));
		assertEquals("DUPLICATE_KEY", exception.getCode());
	}

	/**
	 * Feature: 範本工作表對映管理
	 * Scenario: 操作無效之識別碼 (id <= 0)
	 * Given: 傳入 ID 為 0 或負數
	 * When: 調用 get / update / delete 使用案例
	 * Then: 應拋出 ValidationException 且錯誤代碼為 PARAM_INVALID
	 */
	@Test
	void shouldThrowValidationException_whenIdIsInvalid() {
		GetTemplateSheetMappingQuery getQuery = new GetTemplateSheetMappingQuery(0L);
		ValidationException getEx = assertThrows(ValidationException.class, () -> service.get(getQuery));
		assertEquals("PARAM_INVALID", getEx.getCode());

		UpdateTemplateSheetMappingCommand updateCmd = new UpdateTemplateSheetMappingCommand(
				-1L, "USER_PROFILE", "USER", "使用者", 0, YesNo.Y, YesNo.Y);
		ValidationException updateEx = assertThrows(ValidationException.class, () -> service.update(updateCmd));
		assertEquals("PARAM_INVALID", updateEx.getCode());

		DeleteTemplateSheetMappingCommand deleteCmd = new DeleteTemplateSheetMappingCommand(0L);
		ValidationException deleteEx = assertThrows(ValidationException.class, () -> service.delete(deleteCmd));
		assertEquals("PARAM_INVALID", deleteEx.getCode());
	}
}
