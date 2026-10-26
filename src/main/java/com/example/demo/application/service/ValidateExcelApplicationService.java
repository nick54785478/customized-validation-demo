package com.example.demo.application.service;

import java.io.InputStream;

import org.springframework.stereotype.Service;

import com.example.demo.application.port.in.ValidateExcelUseCase;
import com.example.demo.application.port.out.ExcelValidatorPort;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Excel 範本驗證應用服務 (Validate Excel Application Service)
 * <p>
 * 遵循六角形架構規範，實作 Inbound Port {@link ValidateExcelUseCase} 業務合約。
 * 負責調度 Outbound Port {@link ExcelValidatorPort} 執行實際的 Excel 資料檢核流程。
 * </p>
 */
@Slf4j
@Service
@AllArgsConstructor
public class ValidateExcelApplicationService implements ValidateExcelUseCase {

	private final ExcelValidatorPort excelValidatorPort;

	/**
	 * 執行 Excel 範本校驗
	 *
	 * @param templateName 範本名稱
	 * @param inputStream  Excel 輸入串流
	 */
	@Override
	public void validateExcel(String templateName, InputStream inputStream) {
		log.debug("開始執行範本 [{}] 的 Excel 資料驗證", templateName);
		excelValidatorPort.validateExcelData(templateName, inputStream);
	}
}
