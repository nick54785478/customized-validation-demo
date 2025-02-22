package com.example.demo.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.domain.policy.aggregate.ValidationPolicy;
import com.example.demo.domain.service.ExcelValidateService;
import com.example.demo.domain.share.ContextRoot;
import com.example.demo.domain.share.command.UploadTemplateCommand;
import com.example.demo.exception.ExcelValidationException;
import com.example.demo.infra.repository.ValidationPolicyRepository;
import com.example.demo.share.bean.ValidateErrorProperty;
import com.example.demo.util.ExcelUtil;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.DEFAULT, timeout = 36000, rollbackFor = Exception.class)
public class UploadCommandService {

	private ExcelValidateService excelValidateService;
	private ValidationPolicyRepository validationPolicyRepository;

	/**
	 * 上傳功能
	 * 
	 * @param command
	 * @param file
	 */
	public void upload(UploadTemplateCommand command, MultipartFile file) throws IOException {

		// 取出驗證規則
		List<ValidationPolicy> policyList = validationPolicyRepository.findByTemplateName(command.getName());
		List<String> sheetNameList = policyList.stream().map(ValidationPolicy::getTemplateSheetName)
				.collect(Collectors.toList());

		// 讀取多張表資料
		Map<String, List<Map<String, String>>> excelData = ExcelUtil.readExcelData(file.getInputStream(),
				sheetNameList);
		// 建立 ContextRoot
		ContextRoot contextRoot = ContextRoot.builder().params(new LinkedHashMap<>()).sheetMap(excelData).build();

		// 客製驗證
		List<ValidateErrorProperty> vepList = excelValidateService.validateExcelData(contextRoot, policyList,
				new ArrayList<>());

		if (!vepList.isEmpty()) {
			throw new ExcelValidationException("VALIDATED_FAILED", vepList);
		}

		// TODO 後續上傳功能
		System.out.println(vepList);
	}
}
