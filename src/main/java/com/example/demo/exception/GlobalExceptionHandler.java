package com.example.demo.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.demo.exception.response.BaseExceptionResponse;
import com.example.demo.exception.response.BaseExceptionsResponse;
import com.example.demo.share.bean.ValidateErrorProperty;
import com.example.demo.util.BaseDataTransformer;

import lombok.extern.slf4j.Slf4j;

/**
 * 全域例外處理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<BaseExceptionResponse> handleValidationException(ValidationException e) {
		return ResponseEntity.status(HttpStatus.OK)
				.body(BaseDataTransformer.transformData(e, BaseExceptionResponse.class));
	}

	@ResponseBody
	@ExceptionHandler(ExcelValidationException.class)
	public ResponseEntity<BaseExceptionsResponse> handleExcelValidationException(
			final ExcelValidationException e) {
		List<String> errMessageList = e.getVepList().stream().map(ValidateErrorProperty::getMessage)
				.collect(Collectors.toList());
		return ResponseEntity.status(HttpStatus.OK)
				.body(new BaseExceptionsResponse("VALIDATE_FAILED", errMessageList));
	}

}
