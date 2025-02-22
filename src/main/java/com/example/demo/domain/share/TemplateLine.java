package com.example.demo.domain.share;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 代表 Excel 中的一個欄位 (對應於某個 MappingFieldName)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateLine {
	
	private String sheetName; // Sheet Name

	private String mappingFieldName; // 欄位名稱

	private int dataColumnNum; // 該欄位在 Excel 中的欄號 (1-based，如 A=1, B=2)

}
