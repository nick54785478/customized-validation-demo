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
 * 請購檔案處理器整合測試 (PR Import File Processor Test)
 * <p>
 * 遵循 BDD (Behavior Driven Development) 測試規範與 Gherkin 語法撰寫。
 * 驗證輸入適配器 {@link BatchFileProcessor} 與 {@link PrImportFileProcessor}
 * 在處理本機檔案 (File) 或 MinIO / S3 二進位串流 (InputStream) 時，
 * 透過宣告式 {@link com.example.demo.application.annotation.CustomValidate} 觸發前置檢核的行為。
 * </p>
 */
@SpringBootTest(classes = PrImportFileProcessorTest.TestConfig.class)
class PrImportFileProcessorTest {

	@MockitoBean
	private ValidateExcelUseCase validateExcelUseCase;

	@MockitoBean
	private UploadCommandService uploadCommandService;

	@Autowired
	@Qualifier("prImportFileProcessor")
	private BatchFileProcessor prImportFileProcessor;

	@Configuration
	@EnableAspectJAutoProxy(proxyTargetClass = true)
	@Import({CustomValidationAspect.class, PrImportFileProcessor.class})
	static class TestConfig {
	}

	/**
	 * Feature: 請購批次檔案處理 (File 來源)
	 * Scenario: 傳入合法之請購 Excel 檔案
	 * Given: 於本機生成合法之 PR 請購單 Excel 檔案，且 ValidateExcelUseCase 檢核通過
	 * When: 呼叫 prImportFileProcessor.processFile(file)
	 * Then: 通過 @CustomValidate 切面檢核，調用應用層 uploadCommandService.upload 執行入庫
	 */
	@Test
	@DisplayName("檔案處理: 合法 Excel File 應通過檢核並調用應用層入庫")
	void processFile_shouldSucceed_whenValidExcelFile(@TempDir Path tempDir) throws Exception {
		// Given
		File validFile = tempDir.resolve("PR_VALID.xlsx").toFile();
		createSampleExcel(validFile, "MAT-1001", "MacBook Pro", 5, 60000.0);

		doNothing().when(validateExcelUseCase).validateExcel(eq("PR_IMPORT"), any());
		doNothing().when(uploadCommandService).upload(any(), any());

		// When
		prImportFileProcessor.processFile(validFile);

		// Then
		verify(validateExcelUseCase).validateExcel(eq("PR_IMPORT"), any());
		verify(uploadCommandService).upload(any(), any());
	}

	/**
	 * Feature: 請購批次檔案處理 (File 來源)
	 * Scenario: 傳入不合規之請購 Excel 檔案
	 * Given: 檔案中欄位資料有誤，ValidateExcelUseCase 拋出 ExcelValidationException
	 * When: 呼叫 prImportFileProcessor.processFile(invalidFile)
	 * Then: AOP 切面阻擋執行並拋出 ExcelValidationException，核心應用層 upload 未被執行
	 */
	@Test
	@DisplayName("檔案處理: 不合格 Excel File 應由切面阻擋並拋出例外")
	void processFile_shouldThrowException_whenExcelValidationFails(@TempDir Path tempDir) throws Exception {
		// Given
		File invalidFile = tempDir.resolve("PR_INVALID.xlsx").toFile();
		createSampleExcel(invalidFile, "INVALID_MAT", "Unknown Item", -10, 0.0);

		doThrow(new ExcelValidationException("DATA_VALIDATION_FAILED", List.of(new ValidationError("ERR"))))
				.when(validateExcelUseCase).validateExcel(eq("PR_IMPORT"), any());

		// When & Then
		assertThatThrownBy(() -> prImportFileProcessor.processFile(invalidFile))
				.isInstanceOf(ExcelValidationException.class);

		verify(uploadCommandService, never()).upload(any(), any());
	}

	/**
	 * Feature: 請購批次串流處理 (MinIO / S3 InputStream 來源)
	 * Scenario: 自物件儲存 (如 MinIO) 讀取合法之二進位串流
	 * Given: 合法之 Excel 二進位 ByteArrayInputStream
	 * When: 呼叫 prImportFileProcessor.processStream(inputStream)
	 * Then: 通過 @CustomValidate 切面檢核，調用應用層 uploadCommandService.upload
	 */
	@Test
	@DisplayName("串流處理: 合法 MinIO / S3 二進位 InputStream 應通過檢核並入庫")
	void processStream_shouldSucceed_whenValidInputStream() throws Exception {
		// Given
		byte[] excelBytes = createSampleExcelBytes("MAT-2001", "Dell XPS", 3, 45000.0);
		InputStream inputStream = new ByteArrayInputStream(excelBytes);

		doNothing().when(validateExcelUseCase).validateExcel(eq("PR_IMPORT"), any());
		doNothing().when(uploadCommandService).upload(any(), any());

		// When
		prImportFileProcessor.processStream(inputStream);

		// Then
		verify(validateExcelUseCase).validateExcel(eq("PR_IMPORT"), any());
		verify(uploadCommandService).upload(any(), any());
	}

	/**
	 * Feature: 請購批次串流處理 (MinIO / S3 InputStream 來源)
	 * Scenario: 自物件儲存讀取不合規之二進位串流
	 * Given: ValidateExcelUseCase 拋出 ExcelValidationException
	 * When: 呼叫 prImportFileProcessor.processStream(inputStream)
	 * Then: 切面阻擋並拋出 ExcelValidationException，核心應用層未被執行
	 */
	@Test
	@DisplayName("串流處理: 不合格 MinIO / S3 InputStream 應由切面阻擋")
	void processStream_shouldThrowException_whenStreamValidationFails() throws Exception {
		// Given
		byte[] excelBytes = createSampleExcelBytes("INVALID_MAT", "Unknown", 0, 0.0);
		InputStream inputStream = new ByteArrayInputStream(excelBytes);

		doThrow(new ExcelValidationException("DATA_VALIDATION_FAILED", List.of(new ValidationError("ERR"))))
				.when(validateExcelUseCase).validateExcel(eq("PR_IMPORT"), any());

		// When & Then
		assertThatThrownBy(() -> prImportFileProcessor.processStream(inputStream))
				.isInstanceOf(ExcelValidationException.class);

		verify(uploadCommandService, never()).upload(any(), any());
	}

	/**
	 * Feature: 批次檔案處理器介面多型支援
	 * Scenario: 驗證 BatchFileProcessor 抽象介面合約
	 * Given: 注入之 PrImportFileProcessor 實作 BatchFileProcessor
	 * When: 呼叫 getTemplateName 與 supports
	 * Then: 回傳 "PR_IMPORT" 且正確判定支援包含 PR 之檔案
	 */
	@Test
	@DisplayName("抽象架構驗證: BatchFileProcessor 介面多型與支援度檢查")
	void batchFileProcessor_shouldSupportInterfaceContract(@TempDir Path tempDir) throws Exception {
		assertThat(prImportFileProcessor.getTemplateName()).isEqualTo("PR_IMPORT");
		File supportedFile = tempDir.resolve("PR_20261029.xlsx").toFile();
		File unsupportedFile = tempDir.resolve("OTHER_TEMPLATE.xlsx").toFile();
		supportedFile.createNewFile();
		unsupportedFile.createNewFile();

		assertThat(prImportFileProcessor.supports(supportedFile)).isTrue();
		assertThat(prImportFileProcessor.supports(unsupportedFile)).isFalse();
	}

	/**
	 * 輔助方法：生成測試用 Excel 檔案
	 */
	private void createSampleExcel(File targetFile, String itemNo, String itemName, int quantity, double unitPrice) throws IOException {
		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("請購明細清單");
			Row headerRow = sheet.createRow(0);
			headerRow.createCell(0).setCellValue("物料料號");
			headerRow.createCell(1).setCellValue("物料品名");
			headerRow.createCell(2).setCellValue("規格型號");
			headerRow.createCell(3).setCellValue("需求數量");
			headerRow.createCell(4).setCellValue("預算單價");
			headerRow.createCell(5).setCellValue("成本中心");
			headerRow.createCell(6).setCellValue("需求交期");

			Row dataRow = sheet.createRow(1);
			dataRow.createCell(0).setCellValue(itemNo);
			dataRow.createCell(1).setCellValue(itemName);
			dataRow.createCell(2).setCellValue("標準規格");
			dataRow.createCell(3).setCellValue(quantity);
			dataRow.createCell(4).setCellValue(unitPrice);
			dataRow.createCell(5).setCellValue("CC001");
			dataRow.createCell(6).setCellValue("2026-12-31");

			try (FileOutputStream fos = new FileOutputStream(targetFile)) {
				workbook.write(fos);
			}
		}
	}

	/**
	 * 輔助方法：生成測試用 Excel 二進位位元組陣列 (模擬 MinIO 串流)
	 */
	private byte[] createSampleExcelBytes(String itemNo, String itemName, int quantity, double unitPrice) throws IOException {
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("請購明細清單");
			Row headerRow = sheet.createRow(0);
			headerRow.createCell(0).setCellValue("物料料號");
			headerRow.createCell(1).setCellValue("物料品名");
			headerRow.createCell(2).setCellValue("規格型號");
			headerRow.createCell(3).setCellValue("需求數量");
			headerRow.createCell(4).setCellValue("預算單價");
			headerRow.createCell(5).setCellValue("成本中心");
			headerRow.createCell(6).setCellValue("需求交期");

			Row dataRow = sheet.createRow(1);
			dataRow.createCell(0).setCellValue(itemNo);
			dataRow.createCell(1).setCellValue(itemName);
			dataRow.createCell(2).setCellValue("標準規格");
			dataRow.createCell(3).setCellValue(quantity);
			dataRow.createCell(4).setCellValue(unitPrice);
			dataRow.createCell(5).setCellValue("CC001");
			dataRow.createCell(6).setCellValue("2026-12-31");

			workbook.write(bos);
			return bos.toByteArray();
		}
	}
}
