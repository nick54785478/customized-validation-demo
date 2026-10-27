package com.example.demo.iface.advice;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.demo.application.exception.ExcelValidationException;
import com.example.demo.application.exception.ValidationException;
import com.example.demo.application.shared.vo.ValidationError;
import com.example.demo.iface.resource.out.BaseExceptionResponse;
import com.example.demo.iface.resource.out.BaseExceptionsResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * 表現層全域例外轉譯器 (Global Exception Handler / RestControllerAdvice)
 * <p>
 * 作為表現層輸入適配器 (Inbound Adapter) 的防護網。
 * 負責攔截應用層拋出之業務與檢核例外（如 {@link ValidationException}、{@link ExcelValidationException}），
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
	 * @return HTTP 200 封裝之錯誤回應
	 */
	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<BaseExceptionResponse> handleValidationException(ValidationException e) {
		log.warn("捕獲通用驗證例外: code={}, message={}", e.getCode(), e.getMessage());
		return ResponseEntity.status(HttpStatus.OK)
				.body(new BaseExceptionResponse(e.getCode(), e.getMessage()));
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
