package com.example.demo.service;

import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.domain.share.command.UploadTemplateCommand;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.DEFAULT, timeout = 36000, rollbackFor = Exception.class)
public class UploadCommandService {

	/**
	 * 上傳功能
	 * 
	 * @param command
	 * @param file
	 */
	public void upload(UploadTemplateCommand command, MultipartFile file) throws IOException {

		log.debug("command:{}, file:{}", command, file);

//		// 取出驗證規則
//		List<ValidationPolicy> policyList = validationPolicyRepository.findByTemplateName(command.getName());
//		List<String> sheetNameList = policyList.stream().map(ValidationPolicy::getTemplateSheetName)
//				.collect(Collectors.toList());
//
//		// 讀取多張表資料
//		Map<String, List<Map<String, String>>> excelData = ExcelUtil.readExcelData(file.getInputStream(),
//				sheetNameList);
//		// 建立 ContextRoot
//		ContextRoot contextRoot = ContextRoot.builder().params(new LinkedHashMap<>()).sheetMap(excelData).build();
//
//		// 客製驗證
//		List<ValidateErrorProperty> vepList = excelValidateService.validateExcelData(contextRoot, policyList,
//				new ArrayList<>());
//
//		if (!vepList.isEmpty()) {
//			throw new ExcelValidationException("VALIDATED_FAILED", vepList);
//		}

		// TODO 後續上傳功能
//		System.out.println(vepList);
	}

	/**
	 * 進行客製驗證
	 * 
	 * @param
	 */
	public void validate() {

	}
}
