package com.example.demo.util;

import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Excel 欄位轉換工具類
 */
@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExcelAddressParser {

	/**
	 * 轉換行號與欄號為 Excel Cell Address (如 A1, B2, C3)
	 * 
	 * @param row Excel 列號 (從 1 開始)
	 * @param col Excel 欄號 (從 1 開始)，該欄位在 Excel 中的欄號 (1-based，如 A=1, B=2)
	 * @return String 例如 "B2", "C3"
	 */
	public static String convertNumToAddress(Integer row, Integer col) {
		if (row == null || col == null || row < 1 || col < 1) {
			return "Invalid Cell"; // 避免 null 或非法數值
		}
		return convertColumnToLetter(col) + row;
	}

	/**
	 * 將數字欄號轉換為 Excel 字母 (例如: 1 -> A, 2 -> B, 27 -> AA)
	 * 
	 * @param col 欄號 (從 1 開始)
	 * @return Excel 欄位名稱 (如 "A", "B", "AA", "AB")
	 */
	private static String convertColumnToLetter(int col) {
		StringBuilder columnName = new StringBuilder();
		while (col > 0) {
			col--; // 調整索引 (Excel 是 1-based)
			columnName.insert(0, (char) ('A' + (col % 26)));
			col /= 26;
		}
		return columnName.toString();
	}

}
