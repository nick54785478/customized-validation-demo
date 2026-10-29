package com.example.demo.iface.schedule.processor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.demo.application.exception.ExcelValidationException;
import com.example.demo.application.port.in.ValidateExcelUseCase;
import com.example.demo.application.service.UploadCommandService;
import com.example.demo.application.shared.vo.ValidationError;
import com.example.demo.iface.aspect.CustomValidationAspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 採購訂單檔案處理器整合測試 (PO Import File Processor Test)
 * <p>
 * 遵循 BDD (Behavior Driven Development) 測試規範與 Gherkin 語法撰寫。<br>
 * 驗證輸入適配器 {@link BatchFileProcessor} 與 {@link PoImportFileProcessor}
 * 在處理本機檔案 (File) 或 MinIO / AWS S3 二進位串流 (InputStream) 時，
 * 透過宣告式 {@link com.example.demo.application.annotation.CustomValidate} 觸發前置檢核的行為。
 * </p>
 */
@SpringBootTest(classes = PoImportFileProcessorTest.TestConfig.class)
class PoImportFileProcessorTest {

	@MockitoBean
	private ValidateExcelUseCase validateExcelUseCase;

	@MockitoBean
	private UploadCommandService uploadCommandService;

	@Autowired
	@Qualifier("poImportFileProcessor")
	private BatchFileProcessor poImportFileProcessor;

	@Configuration
	@EnableAspectJAutoProxy(proxyTargetClass = true)
	@Import({CustomValidationAspect.class, PoImportFileProcessor.class})
	static class TestConfig {
	}

	/**
	 * Feature: 採購訂單批次檔案處理 (File 來源)
	 * Scenario: 傳入合法之採購訂單 Excel 檔案
	 * Given: 於本機生成合法之 PO 採購訂單 Excel 檔案，且 ValidateExcelUseCase 檢核通過
	 * When: 呼叫 poImportFileProcessor.processFile(file)
	 * Then: 通過 @CustomValidate 切面檢核，調用應用層 uploadCommandService.upload 執行入庫
	 */
	@Test
	@DisplayName("檔案處理: 合法 Excel File 應通過檢核並調用應用層入庫")
	void processFile_shouldSucceed_whenValidExcelFile(@TempDir Path tempDir) throws Exception {
		// Given
		File validFile = tempDir.resolve("PO_VALID.xlsx").toFile();
		createSampleExcel(validFile, "PO-2026-0001", "10", "V1001", "MAT-9001", "伺服馬達", 5, 45000.0);

		doNothing().when(validateExcelUseCase).validateExcel(eq("PO_IMPORT"), any());
		doNothing().when(uploadCommandService).upload(any(), any());

		// When
		poImportFileProcessor.processFile(validFile);

		// Then
		verify(validateExcelUseCase).validateExcel(eq("PO_IMPORT"), any());
		verify(uploadCommandService).upload(any(), any());
	}

	/**
	 * Feature: 採購訂單批次檔案處理 (File 來源)
	 * Scenario: 傳入不合規之採購訂單 Excel 檔案
	 * Given: 檔案中欄位資料有誤，ValidateExcelUseCase 拋出 ExcelValidationException
	 * When: 呼叫 poImportFileProcessor.processFile(invalidFile)
	 * Then: AOP 切面阻擋執行並拋出 ExcelValidationException，核心應用層 upload 未被執行
	 */
	@Test
	@DisplayName("檔案處理: 不合規 Excel File 應被切面攔截並阻擋應用層入庫")
	void processFile_shouldThrowException_whenValidationFails(@TempDir Path tempDir) throws Exception {
		// Given
		File invalidFile = tempDir.resolve("PO_INVALID.xlsx").toFile();
		createSampleExcel(invalidFile, "INVALID-PO", "10", "V9999", "MAT-9001", "伺服馬達", -5, 0.0);

		List<ValidationError> errors = List.of(
				new ValidationError("PO_ITEM", 1, "採購單號格式不合規"),
				new ValidationError("PO_ITEM", 1, "採購單價必須為正數")
		);
		doThrow(new ExcelValidationException("驗證失敗", errors))
				.when(validateExcelUseCase).validateExcel(eq("PO_IMPORT"), any());

		// When & Then
		assertThatThrownBy(() -> poImportFileProcessor.processFile(invalidFile))
				.isInstanceOf(ExcelValidationException.class);

		verify(validateExcelUseCase).validateExcel(eq("PO_IMPORT"), any());
		verify(uploadCommandService, never()).upload(any(), any());
	}

	/**
	 * Feature: 採購訂單 MinIO / S3 二進位串流處理 (Stream 來源)
	 * Scenario: 傳入合法之採購訂單 InputStream
	 * Given: 記憶體中構建合法 PO 試算表串流，且 ValidateExcelUseCase 檢核通過
	 * When: 呼叫 poImportFileProcessor.processStream(stream)
	 * Then: 通過 @CustomValidate 切面檢核，且串流經自動重設後成功傳遞至應用層 upload
	 */
	@Test
	@DisplayName("串流處理: 合法 InputStream 應通過檢核並調用應用層入庫")
	void processStream_shouldSucceed_whenValidStream() throws Exception {
		// Given
		byte[] excelBytes = createSampleExcelBytes("PO-2026-0002", "10", "V1002", "MAT-9002", "變頻器", 2, 18000.0);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(excelBytes);

		doNothing().when(validateExcelUseCase).validateExcel(eq("PO_IMPORT"), any());
		doNothing().when(uploadCommandService).upload(any(), any());

		// When
		poImportFileProcessor.processStream(inputStream);

		// Then
		verify(validateExcelUseCase).validateExcel(eq("PO_IMPORT"), any());
		verify(uploadCommandService).upload(any(), any());
	}

	/**
	 * Feature: 採購訂單 MinIO / S3 二進位串流處理 (Stream 來源)
	 * Scenario: 傳入不合規之採購訂單 InputStream
	 * Given: 串流資料檢核失敗，ValidateExcelUseCase 拋出 ExcelValidationException
	 * When: 呼叫 poImportFileProcessor.processStream(invalidStream)
	 * Then: AOP 切面拋出例外，且應用層 upload 未被執行
	 */
	@Test
	@DisplayName("串流處理: 不合規 InputStream 應被切面攔截並拋出例外")
	void processStream_shouldThrowException_whenValidationFails() throws Exception {
		// Given
		byte[] invalidBytes = createSampleExcelBytes("INVALID-PO", "10", "V9999", "MAT-9002", "變頻器", -1, 0.0);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(invalidBytes);

		List<ValidationError> errors = List.of(
				new ValidationError("PO_ITEM", 1, "採購數量不能為負數")
		);
		doThrow(new ExcelValidationException("串流檢核失敗", errors))
				.when(validateExcelUseCase).validateExcel(eq("PO_IMPORT"), any());

		// When & Then
		assertThatThrownBy(() -> poImportFileProcessor.processStream(inputStream))
				.isInstanceOf(ExcelValidationException.class);

		verify(validateExcelUseCase).validateExcel(eq("PO_IMPORT"), any());
		verify(uploadCommandService, never()).upload(any(), any());
	}

	/**
	 * Feature: 批次檔案適配過濾 (supports)
	 * Scenario: 驗證檔案名稱模式比對
	 * Given: 檔名為 PO 開頭之 Excel 檔案，以及其他無關副檔名或非 PO 開頭檔案
	 * When: 呼叫 poImportFileProcessor.supports(file)
	 * Then: 僅有合規之 PO 試算表回傳 true，其餘回傳 false
	 */
	@Test
	@DisplayName("適配過濾: 僅支援 PO 開頭且副檔名為 xlsx/xls 之檔案")
	void supports_shouldFilterByPrefixAndExtension(@TempDir Path tempDir) throws IOException {
		// Given
		File poFile = tempDir.resolve("PO_20261029.xlsx").toFile();
		poFile.createNewFile();

		File lowerPoFile = tempDir.resolve("po_order.xls").toFile();
		lowerPoFile.createNewFile();

		File prFile = tempDir.resolve("PR_20261029.xlsx").toFile();
		prFile.createNewFile();

		File txtFile = tempDir.resolve("PO_order.txt").toFile();
		txtFile.createNewFile();

		// Then
		assertThat(poImportFileProcessor.supports(poFile)).isTrue();
		assertThat(poImportFileProcessor.supports(lowerPoFile)).isTrue();
		assertThat(poImportFileProcessor.supports(prFile)).isFalse();
		assertThat(poImportFileProcessor.supports(txtFile)).isFalse();
		assertThat(poImportFileProcessor.supports(null)).isFalse();
		assertThat(poImportFileProcessor.getTemplateName()).isEqualTo("PO_IMPORT");
	}

	// -------------------------------------------------------------------------
	// 測試輔助方法
	// -------------------------------------------------------------------------

	private void createSampleExcel(File targetFile, String poNo, String lineNo, String vendorCode,
								   String itemNo, String itemName, int qty, double price) throws IOException {
		try (Workbook workbook = new XSSFWorkbook();
			 FileOutputStream fos = new FileOutputStream(targetFile)) {
			Sheet sheet = workbook.createSheet("採購訂單明細");
			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("採購單號");
			header.createCell(1).setCellValue("項次");
			header.createCell(2).setCellValue("供應商代碼");
			header.createCell(3).setCellValue("物料料號");
			header.createCell(4).setCellValue("物料品名");
			header.createCell(5).setCellValue("採購數量");
			header.createCell(6).setCellValue("計量單位");
			header.createCell(7).setCellValue("採購單價");
			header.createCell(8).setCellValue("幣別");
			header.createCell(9).setCellValue("約定交期");

			Row data = sheet.createRow(1);
			data.createCell(0).setCellValue(poNo);
			data.createCell(1).setCellValue(lineNo);
			data.createCell(2).setCellValue(vendorCode);
			data.createCell(3).setCellValue(itemNo);
			data.createCell(4).setCellValue(itemName);
			data.createCell(5).setCellValue(String.valueOf(qty));
			data.createCell(6).setCellValue("SET");
			data.createCell(7).setCellValue(String.valueOf(price));
			data.createCell(8).setCellValue("TWD");
			data.createCell(9).setCellValue("2026-12-31");

			workbook.write(fos);
		}
	}

	private byte[] createSampleExcelBytes(String poNo, String lineNo, String vendorCode,
										  String itemNo, String itemName, int qty, double price) throws IOException {
		try (Workbook workbook = new XSSFWorkbook();
			 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("採購訂單明細");
			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("採購單號");
			header.createCell(1).setCellValue("項次");
			header.createCell(2).setCellValue("供應商代碼");
			header.createCell(3).setCellValue("物料料號");
			header.createCell(4).setCellValue("物料品名");
			header.createCell(5).setCellValue("採購數量");
			header.createCell(6).setCellValue("計量單位");
			header.createCell(7).setCellValue("採購單價");
			header.createCell(8).setCellValue("幣別");
			header.createCell(9).setCellValue("約定交期");

			Row data = sheet.createRow(1);
			data.createCell(0).setCellValue(poNo);
			data.createCell(1).setCellValue(lineNo);
			data.createCell(2).setCellValue(vendorCode);
			data.createCell(3).setCellValue(itemNo);
			data.createCell(4).setCellValue(itemName);
			data.createCell(5).setCellValue(String.valueOf(qty));
			data.createCell(6).setCellValue("SET");
			data.createCell(7).setCellValue(String.valueOf(price));
			data.createCell(8).setCellValue("TWD");
			data.createCell(9).setCellValue("2026-12-31");

			workbook.write(bos);
			return bos.toByteArray();
		}
	}
}
