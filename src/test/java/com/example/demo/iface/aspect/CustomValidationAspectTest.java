package com.example.demo.iface.aspect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.demo.application.annotation.CustomValidate;
import com.example.demo.application.exception.ExcelValidationException;
import com.example.demo.application.exception.ValidationException;
import com.example.demo.application.port.in.ValidateExcelUseCase;
import com.example.demo.application.shared.command.UploadTemplateCommand;
import com.example.demo.application.shared.vo.ValidationError;
import com.example.demo.iface.resource.in.UploadTemplateResource;

/**
 * 宣告式客製化驗證切面單元/整合測試 (Custom Validation Aspect Test)
 * <p>
 * 遵循 BDD (Behavior Driven Development) 測試規範與 Gherkin 語法撰寫測試案例。
 * 驗證宣告式註解 {@link CustomValidate} 於靜態名稱、SpEL 表達式、自動探測與多檔案來源 (MultipartFile, InputStream, byte[]) 下的攔截行為。
 * </p>
 */
@SpringBootTest(classes = CustomValidationAspectTest.TestConfig.class)
class CustomValidationAspectTest {

	@MockitoBean
	private ValidateExcelUseCase validateExcelUseCase;

	@Autowired
	private DummyTestService dummyTestService;

	@Configuration
	@EnableAspectJAutoProxy
	@Import(CustomValidationAspect.class)
	static class TestConfig {
		@Bean
		public DummyTestService dummyTestService() {
			return new DummyTestService();
		}
	}

	/**
	 * 供測試切面攔截使用之假服務元件
	 */
	@Component
	static class DummyTestService {

		@CustomValidate(templateName = "PR_IMPORT")
		public String staticTemplateMethod(MockMultipartFile file) {
			return "OK_STATIC";
		}

		@CustomValidate(templateName = "#resource.name")
		public String spelResourceMethod(UploadTemplateResource resource, MockMultipartFile file) {
			return "OK_SPEL_RESOURCE";
		}

		@CustomValidate
		public String autoDetectMethod(UploadTemplateCommand command, MockMultipartFile file) {
			return "OK_AUTO_DETECT";
		}

		@CustomValidate(templateName = "STREAM_TEST")
		public String inputStreamMethod(InputStream inputStream) throws Exception {
			byte[] bytes = inputStream.readAllBytes();
			return new String(bytes, StandardCharsets.UTF_8);
		}

		@CustomValidate(templateName = "BYTE_TEST")
		public String byteArrayMethod(byte[] data) {
			return "OK_BYTES:" + data.length;
		}

		@CustomValidate
		public String unresolvedMethod(String otherArg, MockMultipartFile file) {
			return "OK_UNRESOLVED";
		}

		@CustomValidate(templateName = "PR_IMPORT")
		public String noFileMethod(String name) {
			return "OK_NO_FILE";
		}
	}

	/**
	 * Feature: 宣告式 Excel 客製化驗證 AOP 切面
	 * Scenario: 靜態範本名稱與合法 MultipartFile 參數
	 * Given: 目標方法標註 @CustomValidate(templateName = "PR_IMPORT") 且傳入有效檔案
	 * When: 呼叫目標方法
	 * Then: 切面應成功解析範本為 "PR_IMPORT"，委託 ValidateExcelUseCase 進行校驗並放行目標方法
	 */
	@Test
	void validateExcelData_shouldPass_whenStaticTemplateAndValidFile() {
		doNothing().when(validateExcelUseCase).validateExcel(eq("PR_IMPORT"), any(InputStream.class));

		MockMultipartFile file = new MockMultipartFile(
				"file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				"mock-excel-content".getBytes(StandardCharsets.UTF_8));

		String result = dummyTestService.staticTemplateMethod(file);

		assertEquals("OK_STATIC", result);
		verify(validateExcelUseCase).validateExcel(eq("PR_IMPORT"), any(InputStream.class));
	}

	/**
	 * Feature: 宣告式 Excel 客製化驗證 AOP 切面
	 * Scenario: 使用 SpEL 表達式動態解析 Resource 物件中的範本名稱
	 * Given: 目標方法標註 @CustomValidate(templateName = "#resource.name") 且傳入 UploadTemplateResource
	 * When: 呼叫目標方法
	 * Then: 切面應動態評估 SpEL 表達式取得範本名稱 "USER_PROFILE"，並調用驗證使用案例
	 */
	@Test
	void validateExcelData_shouldPass_whenSpelTemplateExpression() {
		doNothing().when(validateExcelUseCase).validateExcel(eq("USER_PROFILE"), any(InputStream.class));

		UploadTemplateResource resource = new UploadTemplateResource(
				"USER_PROFILE", "USER", "xlsx", "/tmp", "user.xlsx");
		MockMultipartFile file = new MockMultipartFile(
				"file", "user.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				"mock-user-excel".getBytes(StandardCharsets.UTF_8));

		String result = dummyTestService.spelResourceMethod(resource, file);

		assertEquals("OK_SPEL_RESOURCE", result);
		verify(validateExcelUseCase).validateExcel(eq("USER_PROFILE"), any(InputStream.class));
	}

	/**
	 * Feature: 宣告式 Excel 客製化驗證 AOP 切面
	 * Scenario: 註解未配置 templateName 時自動探測入參物件
	 * Given: 目標方法標註 @CustomValidate 但未指定名稱，入參包含具有 name() 方法之 UploadTemplateCommand
	 * When: 呼叫目標方法
	 * Then: 切面應自動探測出範本名稱 "PR_IMPORT" 並調用驗證使用案例
	 */
	@Test
	void validateExcelData_shouldPass_whenAutoDetectTemplateFromCommand() {
		doNothing().when(validateExcelUseCase).validateExcel(eq("PR_IMPORT"), any(InputStream.class));

		UploadTemplateCommand command = new UploadTemplateCommand(
				"PR_IMPORT", "PR", "xlsx", "/tmp", "pr.xlsx");
		MockMultipartFile file = new MockMultipartFile(
				"file", "pr.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				"mock-pr-excel".getBytes(StandardCharsets.UTF_8));

		String result = dummyTestService.autoDetectMethod(command, file);

		assertEquals("OK_AUTO_DETECT", result);
		verify(validateExcelUseCase).validateExcel(eq("PR_IMPORT"), any(InputStream.class));
	}

	/**
	 * Feature: 宣告式 Excel 客製化驗證 AOP 切面
	 * Scenario: 傳入標準 Java InputStream（解耦 Web 技術物件）
	 * Given: 目標方法標註 @CustomValidate(templateName = "STREAM_TEST") 且入參為純 InputStream
	 * When: 呼叫目標方法
	 * Then: 切面應執行驗證且替換為重置後之串流，使下游目標方法仍可完整讀取資料
	 */
	@Test
	void validateExcelData_shouldPass_whenInputStreamArgument() throws Exception {
		doNothing().when(validateExcelUseCase).validateExcel(eq("STREAM_TEST"), any(InputStream.class));

		byte[] originalBytes = "stream-content-data".getBytes(StandardCharsets.UTF_8);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(originalBytes);

		String result = dummyTestService.inputStreamMethod(inputStream);

		assertEquals("stream-content-data", result);
		verify(validateExcelUseCase).validateExcel(eq("STREAM_TEST"), any(InputStream.class));
	}

	/**
	 * Feature: 宣告式 Excel 客製化驗證 AOP 切面
	 * Scenario: 傳入 byte[] 位元組陣列
	 * Given: 目標方法標註 @CustomValidate("BYTE_TEST") 且入參為位元組陣列
	 * When: 呼叫目標方法
	 * Then: 切面應將 byte[] 轉換為 ByteArrayInputStream 進行驗證並順利放行
	 */
	@Test
	void validateExcelData_shouldPass_whenByteArrayArgument() {
		doNothing().when(validateExcelUseCase).validateExcel(eq("BYTE_TEST"), any(InputStream.class));

		byte[] data = new byte[]{1, 2, 3, 4, 5};
		String result = dummyTestService.byteArrayMethod(data);

		assertEquals("OK_BYTES:5", result);
		verify(validateExcelUseCase).validateExcel(eq("BYTE_TEST"), any(InputStream.class));
	}

	/**
	 * Feature: 宣告式 Excel 客製化驗證 AOP 切面
	 * Scenario: 上傳檔案為空 (empty file)
	 * Given: 傳入長度為 0 之 MultipartFile
	 * When: 呼叫目標方法
	 * Then: 切面應立即攔截並拋出 ValidationException (代碼為 PARAM_INVALID)
	 */
	@Test
	void validateExcelData_shouldThrowValidationException_whenFileIsEmpty() {
		MockMultipartFile emptyFile = new MockMultipartFile(
				"file", "empty.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				new byte[0]);

		ValidationException exception = assertThrows(ValidationException.class, () ->
				dummyTestService.staticTemplateMethod(emptyFile));

		assertEquals("PARAM_INVALID", exception.getCode());
		assertEquals("上傳之 Excel 檔案不可為空", exception.getMessage());
	}

	/**
	 * Feature: 宣告式 Excel 客製化驗證 AOP 切面
	 * Scenario: Excel 內容校驗未通過
	 * Given: ValidateExcelUseCase 拋出 ExcelValidationException
	 * When: 切面執行驗證
	 * Then: 切面應立即阻斷目標方法，並向上拋出 ExcelValidationException 由全域處理器轉譯
	 */
	@Test
	void validateExcelData_shouldPropagateException_whenValidationFails() {
		doThrow(new ExcelValidationException("VALIDATE_FAILED", List.of(
				new ValidationError("D3 需求數量必須大於 0")
		))).when(validateExcelUseCase).validateExcel(eq("PR_IMPORT"), any(InputStream.class));

		MockMultipartFile file = new MockMultipartFile(
				"file", "invalid.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				"mock-content".getBytes(StandardCharsets.UTF_8));

		ExcelValidationException exception = assertThrows(ExcelValidationException.class, () ->
				dummyTestService.staticTemplateMethod(file));

		assertEquals("VALIDATE_FAILED", exception.getCode());
		assertNotNull(exception.getVepList());
		assertEquals(1, exception.getVepList().size());
	}

	/**
	 * Feature: 宣告式 Excel 客製化驗證 AOP 切面
	 * Scenario: 無法解析範本名稱且參數無法自動探測
	 * Given: 註解未配置 templateName 且參數無任何範本名稱提供者
	 * When: 呼叫目標方法
	 * Then: 應拋出 IllegalArgumentException 提示開發者
	 */
	@Test
	void validateExcelData_shouldThrowIllegalArgumentException_whenTemplateNameCannotBeResolved() {
		MockMultipartFile file = new MockMultipartFile(
				"file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				"mock-content".getBytes(StandardCharsets.UTF_8));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
				dummyTestService.unresolvedMethod("some-other-arg", file));

		assertEquals("@CustomValidate 無法解析範本名稱，請透過 templateName 屬性指定或確保入參包含範本名稱", exception.getMessage());
	}

	/**
	 * Feature: 宣告式 Excel 客製化驗證 AOP 切面
	 * Scenario: 目標方法未包含任何可識別的檔案入參
	 * Given: 標註 @CustomValidate 但入參僅有 String
	 * When: 呼叫目標方法
	 * Then: 應拋出 IllegalArgumentException 提示缺少檔案來源
	 */
	@Test
	void validateExcelData_shouldThrowIllegalArgumentException_whenNoFileSource() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
				dummyTestService.noFileMethod("some-name"));

		assertEquals("@CustomValidate 攔截之方法未包含任何可識別的 Excel 檔案來源 (需為 MultipartFile, InputStream, byte[], Resource 或 File)", exception.getMessage());
	}
}
