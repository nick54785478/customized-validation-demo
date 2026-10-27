package com.example.demo.iface.advice;

import java.util.List;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import com.example.demo.application.exception.ExcelValidationException;
import com.example.demo.application.exception.ValidationException;
import com.example.demo.application.shared.vo.ValidationError;
import com.example.demo.iface.resource.out.BaseExceptionResponse;
import com.example.demo.iface.resource.out.BaseExceptionsResponse;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * 表現層全域例外轉譯器 (Global Exception Handler / RestControllerAdvice)
 * <p>
 * 作為表現層輸入適配器 (Inbound Adapter) 的防護網。
 * 負責攔截應用層與表現層拋出之業務與檢核例外（如 {@link ValidationException}、{@link ExcelValidationException}、
 * {@link MethodArgumentNotValidException}、{@link ConstraintViolationException}），
 * 並將其轉譯為標準 HTTP 狀態碼與 JSON 格式錯誤回應資源 ({@link BaseExceptionResponse}、{@link BaseExceptionsResponse})。
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 處理通用業務驗證例外
	 *
	 * @param e 應用層驗證例外
	 * @return HTTP 狀態碼對應之錯誤回應 (NOT_FOUND -> 404, 其他 -> 400)
	 */
	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<BaseExceptionResponse> handleValidationException(ValidationException e) {
		log.warn("捕獲通用驗證例外: code={}, message={}", e.getCode(), e.getMessage());
		HttpStatus status = "NOT_FOUND".equalsIgnoreCase(e.getCode()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
		return ResponseEntity.status(status)
				.body(new BaseExceptionResponse(e.getCode(), e.getMessage()));
	}

	/**
	 * 處理 Spring Validation 物件參數校驗失敗例外 (MethodArgumentNotValidException)
	 *
	 * @param e 參數校驗例外
	 * @return HTTP 400 封裝之參數錯誤回應清單
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<BaseExceptionsResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
		log.warn("捕獲 Spring Bean 參數校驗例外: {}", e.getMessage());
		List<String> errMessageList = e.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.toList();
		if (errMessageList.isEmpty()) {
			errMessageList = e.getBindingResult().getAllErrors().stream()
					.map(DefaultMessageSourceResolvable::getDefaultMessage)
					.toList();
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new BaseExceptionsResponse("PARAM_VALIDATION_FAILED", errMessageList));
	}

	/**
	 * 處理 Spring 6+ 處理器方法參數校驗失敗例外 (HandlerMethodValidationException)
	 *
	 * @param e 方法參數校驗例外
	 * @return HTTP 400 封裝之參數錯誤回應清單
	 */
	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<BaseExceptionsResponse> handleHandlerMethodValidationException(HandlerMethodValidationException e) {
		log.warn("捕獲處理器方法參數校驗例外: {}", e.getMessage());
		List<String> errMessageList = e.getAllErrors().stream()
				.map(MessageSourceResolvable::getDefaultMessage)
				.toList();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new BaseExceptionsResponse("PARAM_VALIDATION_FAILED", errMessageList));
	}

	/**
	 * 處理 Jakarta Bean Validation 限制條件違反例外 (ConstraintViolationException)
	 *
	 * @param e 限制條件違反例外
	 * @return HTTP 400 封裝之參數錯誤回應清單
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<BaseExceptionsResponse> handleConstraintViolationException(ConstraintViolationException e) {
		log.warn("捕獲 Bean Validation 限制違反例外: {}", e.getMessage());
		List<String> errMessageList = e.getConstraintViolations().stream()
				.map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
				.toList();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new BaseExceptionsResponse("PARAM_VALIDATION_FAILED", errMessageList));
	}

	/**
	 * 處理不合法參數例外 (IllegalArgumentException)
	 *
	 * @param e 不合法參數例外
	 * @return HTTP 400 錯誤回應
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<BaseExceptionResponse> handleIllegalArgumentException(IllegalArgumentException e) {
		log.warn("捕獲不合法參數例外: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new BaseExceptionResponse("PARAM_INVALID", e.getMessage()));
	}

	/**
	 * 處理 Excel 範本檢核失敗結構化例外
	 *
	 * @param e Excel 檢核例外
	 * @return HTTP 422 封裝之多筆錯誤回應清單
	 */
	@ResponseBody
	@ExceptionHandler(ExcelValidationException.class)
	public ResponseEntity<BaseExceptionsResponse> handleExcelValidationException(final ExcelValidationException e) {
		log.warn("捕獲 Excel 檢核失敗例外: code={}, 錯誤筆數={}", e.getCode(), e.getVepList() != null ? e.getVepList().size() : 0);
		List<String> errMessageList = e.getVepList().stream()
				.map(ValidationError::message)
				.toList();
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
				.body(new BaseExceptionsResponse("VALIDATE_FAILED", errMessageList));
	}

}
