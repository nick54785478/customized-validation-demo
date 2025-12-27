package com.example.demo.iface.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.application.port.ExcelValidatorPort;
import com.example.demo.share.command.UploadTemplateCommand;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 針對 upload 方法的攔截切面，會在此進行客製驗證
 */
@Slf4j
@Aspect
@Component
@AllArgsConstructor
public class CustomValidationAspect {

	private ExcelValidatorPort excelValidatorAdapter;

	/**
	 * 定義切入點，針對 UploadCommandService 的 upload 方法進行切入。
	 */
	@Pointcut("execution(* com.example.demo.application.service.UploadCommandService.upload(..))")
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

		// 執行客製驗證
		excelValidatorAdapter.validateExcelData(command.getName(), file);

		// 執行後續流程
		return joinPoint.proceed();
	}
}
