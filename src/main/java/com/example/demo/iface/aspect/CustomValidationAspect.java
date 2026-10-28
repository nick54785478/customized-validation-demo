package com.example.demo.iface.aspect;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.Resource;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.application.annotation.CustomValidate;
import com.example.demo.application.exception.ValidationException;
import com.example.demo.application.port.in.ValidateExcelUseCase;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 宣告式客製化 Excel 驗證 AOP 切面 (Custom Validation Aspect - Inbound Adapter)
 * <p>
 * 遵循六角形架構與 Clean Architecture 規範，作為表現層輸入適配器的宣告式攔截防線。
 * 攔截標註 {@link CustomValidate} 註解之方法（如 Controller 入口端點或 Service 方法），
 * 自動解析範本名稱與 Excel 檔案來源，調用應用層 Inbound Port {@link ValidateExcelUseCase} 執行驗證，
 * 嚴守技術解耦與六角形分層原則，達成關注點分離與前置快速失敗 (Fail-Fast)。
 * </p>
 */
@Slf4j
@Aspect
@Component
@AllArgsConstructor
public class CustomValidationAspect {

	private final ValidateExcelUseCase validateExcelUseCase;
	private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
	private final ExpressionParser expressionParser = new SpelExpressionParser();

	/**
	 * 定義切入點：攔截標註有 @CustomValidate 之方法或類別
	 */
	@Pointcut("@annotation(com.example.demo.application.annotation.CustomValidate) || @within(com.example.demo.application.annotation.CustomValidate)")
	public void customValidatePointcut() {
	}

	/**
	 * 環繞通知：解析方法參數、提取範本名稱與檔案串流並調用驗證使用案例
	 *
	 * @param joinPoint 切入點物件
	 * @return 目標方法執行結果
	 * @throws Throwable 驗證失敗例外或目標方法拋出之異常
	 */
	@Around("customValidatePointcut()")
	public Object validateExcelData(ProceedingJoinPoint joinPoint) throws Throwable {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		Class<?> targetClass = joinPoint.getTarget() != null ? joinPoint.getTarget().getClass() : method.getDeclaringClass();
		Method targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);
		Object[] args = joinPoint.getArgs();

		// 優先取得具體實作方法上的 @CustomValidate 註解，若無則取得介面宣告或類別宣告
		CustomValidate customValidate = AnnotatedElementUtils.findMergedAnnotation(targetMethod, CustomValidate.class);
		if (customValidate == null) {
			customValidate = AnnotatedElementUtils.findMergedAnnotation(method, CustomValidate.class);
		}
		if (customValidate == null) {
			customValidate = AnnotatedElementUtils.findMergedAnnotation(targetClass, CustomValidate.class);
		}
		if (customValidate == null) {
			return joinPoint.proceed();
		}

		// 建構 SpEL 評估上下文 (包含 target / this 變數支援)
		StandardEvaluationContext evaluationContext = buildEvaluationContext(targetMethod, joinPoint.getTarget(), args);

		// 1. 解析範本名稱 (templateName)
		String templateName = resolveTemplateName(customValidate, evaluationContext, args);

		// 2. 解析檔案來源物件並執行驗證
		Object fileObj = resolveFileObject(customValidate, evaluationContext, args);
		executeValidation(templateName, fileObj, args);

		// 3. 驗證通過，放行目標方法
		return joinPoint.proceed(args);
	}

	/**
	 * 構建 SpEL 參數評估上下文
	 *
	 * @param method 目標方法
	 * @param target 目標物件實例 (this / target)
	 * @param args   方法參數值
	 * @return 包含所有參數名稱與值的 EvaluationContext
	 */
	private StandardEvaluationContext buildEvaluationContext(Method method, Object target, Object[] args) {
		StandardEvaluationContext context = new StandardEvaluationContext(target);
		if (target != null) {
			context.setVariable("this", target);
			context.setVariable("target", target);
		}
		String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
		if (paramNames != null && args != null) {
			for (int i = 0; i < paramNames.length && i < args.length; i++) {
				context.setVariable(paramNames[i], args[i]);
			}
		}
		return context;
	}

	/**
	 * 解析範本唯一識別名稱
	 *
	 * @param customValidate    註解定義
	 * @param evaluationContext SpEL 上下文
	 * @param args              方法入參
	 * @return 解析出之範本名稱
	 */
	private String resolveTemplateName(CustomValidate customValidate, StandardEvaluationContext evaluationContext, Object[] args) {
		String templateNameExpr = customValidate.templateName();
		if (templateNameExpr == null || templateNameExpr.isBlank()) {
			templateNameExpr = customValidate.value();
		}

		String templateName = null;
		if (templateNameExpr != null && !templateNameExpr.isBlank()) {
			if (templateNameExpr.startsWith("#")) {
				// SpEL 表達式解析
				templateName = expressionParser.parseExpression(templateNameExpr).getValue(evaluationContext, String.class);
			} else {
				// 靜態字串常數
				templateName = templateNameExpr.trim();
			}
		}

		// 若未顯式指定，自動探測入參物件
		if (templateName == null || templateName.isBlank()) {
			templateName = autoDetectTemplateName(args);
		}

		if (templateName == null || templateName.isBlank()) {
			throw new IllegalArgumentException("@CustomValidate 無法解析範本名稱，請透過 templateName 屬性指定或確保入參包含範本名稱");
		}

		return templateName;
	}

	/**
	 * 自動探測入參中之範本名稱
	 *
	 * @param args 方法參數
	 * @return 探測到的範本名稱，若無則回傳 null
	 */
	private String autoDetectTemplateName(Object[] args) {
		if (args == null) {
			return null;
		}
		for (Object arg : args) {
			if (arg == null) {
				continue;
			}
			// 透過反射嘗試取得 name() 或 getTemplateName()
			try {
				Method nameMethod = arg.getClass().getMethod("name");
				if (nameMethod.getReturnType().equals(String.class)) {
					String val = (String) nameMethod.invoke(arg);
					if (val != null && !val.isBlank()) {
						return val;
					}
				}
			} catch (Exception ignored) {
				// 忽略無此方法之狀況
			}
			try {
				Method templateNameMethod = arg.getClass().getMethod("getTemplateName");
				if (templateNameMethod.getReturnType().equals(String.class)) {
					String val = (String) templateNameMethod.invoke(arg);
					if (val != null && !val.isBlank()) {
						return val;
					}
				}
			} catch (Exception ignored) {
				// 忽略無此方法之狀況
			}
		}
		return null;
	}

	/**
	 * 解析檔案來源物件 (MultipartFile, InputStream, byte[], Resource, File)
	 *
	 * @param customValidate    註解定義
	 * @param evaluationContext SpEL 上下文
	 * @param args              方法入參
	 * @return 檔案來源物件
	 */
	private Object resolveFileObject(CustomValidate customValidate, StandardEvaluationContext evaluationContext, Object[] args) {
		String fileExpr = customValidate.file();
		Object fileObj = null;

		if (fileExpr != null && !fileExpr.isBlank()) {
			if (fileExpr.startsWith("#")) {
				fileObj = expressionParser.parseExpression(fileExpr).getValue(evaluationContext);
			} else {
				fileObj = evaluationContext.lookupVariable(fileExpr);
			}
		}

		// 若未顯式指定，自動從方法入參中探測支援之檔案型別
		if (fileObj == null && args != null) {
			for (Object arg : args) {
				if (arg instanceof MultipartFile || arg instanceof InputStream || arg instanceof byte[]
						|| arg instanceof Resource || arg instanceof File) {
					fileObj = arg;
					break;
				}
			}
		}

		if (fileObj == null) {
			throw new IllegalArgumentException("@CustomValidate 攔截之方法未包含任何可識別的 Excel 檔案來源 (需為 MultipartFile, InputStream, byte[], Resource 或 File)");
		}

		return fileObj;
	}

	/**
	 * 執行 Excel 檔案資料驗證流程
	 *
	 * @param templateName 範本名稱
	 * @param fileObj      檔案來源物件
	 * @param args         方法入參清單 (當傳入 InputStream 時替換為重置後串流)
	 * @throws Exception 當檔案讀取或驗證失敗時拋出
	 */
	private void executeValidation(String templateName, Object fileObj, Object[] args) throws Exception {
		if (fileObj instanceof MultipartFile multipartFile) {
			if (multipartFile.isEmpty()) {
				throw new ValidationException("PARAM_INVALID", "上傳之 Excel 檔案不可為空");
			}
			try (InputStream inputStream = multipartFile.getInputStream()) {
				log.debug("切面攔截到 MultipartFile 上傳請求，執行範本 [{}] 校驗: filename={}", templateName, multipartFile.getOriginalFilename());
				validateExcelUseCase.validateExcel(templateName, inputStream);
			}
		} else if (fileObj instanceof InputStream is) {
			byte[] bytes = is.readAllBytes();
			if (bytes.length == 0) {
				throw new ValidationException("PARAM_INVALID", "上傳之 Excel 串流不可為空");
			}
			try (InputStream validateStream = new ByteArrayInputStream(bytes)) {
				log.debug("切面攔截到 InputStream 請求，執行範本 [{}] 校驗", templateName);
				validateExcelUseCase.validateExcel(templateName, validateStream);
			}
			// 將原參數替換為可重複讀取之 ByteArrayInputStream，避免下游方法遭逢 Stream Closed 異常
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					if (args[i] == fileObj) {
						args[i] = new ByteArrayInputStream(bytes);
						break;
					}
				}
			}
		} else if (fileObj instanceof byte[] bytes) {
			if (bytes.length == 0) {
				throw new ValidationException("PARAM_INVALID", "上傳之 Excel 位元組陣列不可為空");
			}
			try (InputStream validateStream = new ByteArrayInputStream(bytes)) {
				log.debug("切面攔截到 byte[] 請求，執行範本 [{}] 校驗", templateName);
				validateExcelUseCase.validateExcel(templateName, validateStream);
			}
		} else if (fileObj instanceof Resource resource) {
			try (InputStream inputStream = resource.getInputStream()) {
				log.debug("切面攔截到 Resource 請求，執行範本 [{}] 校驗", templateName);
				validateExcelUseCase.validateExcel(templateName, inputStream);
			}
		} else if (fileObj instanceof File file) {
			if (!file.exists() || file.length() == 0) {
				throw new ValidationException("PARAM_INVALID", "上傳之 Excel 檔案不存在或為空");
			}
			try (InputStream inputStream = new FileInputStream(file)) {
				log.debug("切面攔截到 File 請求，執行範本 [{}] 校驗: path={}", templateName, file.getAbsolutePath());
				validateExcelUseCase.validateExcel(templateName, inputStream);
			}
		}
	}
}
