package com.example.demo.domain.share;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExcelFieldValidator {
	
	private String type;

	private String fieldName; // 欄位名稱
	
	private String spelExpression; // SpEL 驗證條件 (如 "#value != null")
}
