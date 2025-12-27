package com.example.demo.exception;

import java.util.List;

import com.example.demo.infra.cv.shared.ValidateErrorProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 用於定義檢核失敗的 Exception
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ExcelValidationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private String code;

	List<ValidateErrorProperty> vepList;
}
