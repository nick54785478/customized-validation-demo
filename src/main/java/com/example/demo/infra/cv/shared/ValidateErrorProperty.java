package com.example.demo.infra.cv.shared;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 錯誤訊息參數特性
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateErrorProperty {

	private String rule;
	
	private int rowIndex;
	
	private String message;
}
