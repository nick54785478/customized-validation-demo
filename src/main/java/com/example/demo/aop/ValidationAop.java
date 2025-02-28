package com.example.demo.aop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.domain.policy.aggregate.ValidationPolicy;
import com.example.demo.domain.share.ContextRoot;
import com.example.demo.domain.share.command.UploadTemplateCommand;
import com.example.demo.exception.ExcelValidationException;
import com.example.demo.infra.repository.ValidationPolicyRepository;
import com.example.demo.service.ExcelValidateService;
import com.example.demo.share.bean.ValidateErrorProperty;
import com.example.demo.util.ExcelUtil;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 針對 upload 方法的攔截器，在此進行客製驗證
 */
@Slf4j
@Aspect
@Component
@AllArgsConstructor
public class ValidationAop {

	private ExcelValidateService excelValidateService;
	private ValidationPolicyRepository validationPolicyRepository;

	/**
	 * 定義切入點，針對 UploadCommandService 的 upload 方法進行切入。
	 */
	@Pointcut("execution(* com.example.demo.service.UploadCommandService.upload(..))")
	public void pointCut() {

	}

	/**
	 * 執行客製驗證
	 * 
	 * @param joinPoint 切入點
	 * @return 方法執行結果
	 * @throws Throwable 例外
	 */
	@Around("pointCut()")
	public Object validateExcelData(ProceedingJoinPoint joinPoint) throws Throwable {
		Object[] args = joinPoint.getArgs();
		UploadTemplateCommand command = (UploadTemplateCommand) args[0];
		MultipartFile file = (MultipartFile) args[1];
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

		// 拋出例外，將驗證失敗的資料拋出
		if (!vepList.isEmpty()) {
			throw new ExcelValidationException("VALIDATED_FAILED", vepList);
		}
		// 執行後續流程
		return joinPoint.proceed();
	}
}
