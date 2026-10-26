package com.example.demo.iface.aspect;

import java.io.InputStream;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.application.port.in.ValidateExcelUseCase;
import com.example.demo.application.shared.command.UploadTemplateCommand;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 宣告式客製驗證 AOP 切面 (Custom Validation Aspect - Inbound Adapter)
 * <p>
 * 作為表現層輸入適配器，攔截目標服務（例如 {@code UploadCommandService.upload}）的調用。
 * 在此層將 HTTP/Web 層級的 {@link MultipartFile} 技術物件提取出標準 {@link InputStream}，
 * 調用應用層 Inbound Port {@link ValidateExcelUseCase} 執行驗證，嚴守技術解耦與六角形分層原則。
 * </p>
 */
@Slf4j
@Aspect
@Component
@AllArgsConstructor
public class CustomValidationAspect {

	private final ValidateExcelUseCase validateExcelUseCase;

	/**
	 * 定義切入點：針對 UploadCommandService 的 upload 方法進行攔截
	 */
	@Pointcut("execution(* com.example.demo.application.service.UploadCommandService.upload(..))")
	public void pointCut() {

	}

	/**
	 * 環繞通知：執行客製驗證並放行目標方法
	 *
	 * @param joinPoint 切入點
	 * @return 目標方法執行結果
	 * @throws Throwable 驗證失敗或執行例外
	 */
	@Around("pointCut()")
	public Object validateExcelData(ProceedingJoinPoint joinPoint) throws Throwable {
		Object[] args = joinPoint.getArgs();
		UploadTemplateCommand command = (UploadTemplateCommand) args[0];
		MultipartFile file = (MultipartFile) args[1];

		// 在表現層提取串流，阻絕 MultipartFile 技術物件滲透至應用層
		try (InputStream inputStream = file.getInputStream()) {
			log.debug("切面攔截到範本上傳請求，執行客製驗證: templateName={}", command.name());
			validateExcelUseCase.validateExcel(command.name(), inputStream);
		}

		// 驗證通過，執行後續目標方法流程
		return joinPoint.proceed();
	}
}
