package com.example.demo.application.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 通用業務驗證例外 (Application Validation Exception)
 * <p>
 * 位於應用層例外 (Application Exception)，代表系統在執行應用服務或用例時所發生的業務資料檢核失敗。
 * 作為 UseCase 介面合約的一部分，由表現層例外轉譯器統一捕獲並處理。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ValidationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 錯誤代碼 (例如: "VALIDATE_FAILED")
	 */
	private String code;

	/**
	 * 錯誤說明文字
	 */
	private String message;
}
