package com.example.demo.infra.adapter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.application.domain.policy.aggregate.ValidationPolicy;
import com.example.demo.application.port.ExcelValidatorPort;
import com.example.demo.exception.ExcelValidationException;
import com.example.demo.infra.cv.shared.ValidateErrorProperty;
import com.example.demo.infra.cv.shared.context.ContextRoot;
import com.example.demo.infra.cv.validator.CustomValidator;
import com.example.demo.infra.repository.ValidationPolicyRepository;
import com.example.demo.util.ExcelUtil;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
class ExcelValidatorAdapter implements ExcelValidatorPort {

	private CustomValidator customValidator;
	private ValidationPolicyRepository validationPolicyRepository;

	@Override
	public void validateExcelData(String templateName, MultipartFile file) {
		// 取出驗證規則
		List<ValidationPolicy> policyList = validationPolicyRepository.findByTemplateName(templateName);
		List<String> sheetNameList = policyList.stream().map(ValidationPolicy::getTemplateSheetName).toList();

		// 讀取多張表資料
		try {
			Map<String, List<Map<String, String>>> excelData = ExcelUtil.readExcelData(file.getInputStream(),
					sheetNameList);
			// 建立 ContextRoot
			ContextRoot contextRoot = ContextRoot.builder().params(new LinkedHashMap<>()).sheetMap(excelData).build();
			// 執行客製驗證
			List<ValidateErrorProperty> vepList = customValidator.validateExcelData(contextRoot, policyList);

			// 拋出例外，將驗證失敗的資料拋出
			if (!vepList.isEmpty()) {
				throw new ExcelValidationException("VALIDATED_FAILED", vepList);
			}

		} catch (IOException e) {
			log.error("讀取檔案發生錯誤", e);
		}

	}

}
