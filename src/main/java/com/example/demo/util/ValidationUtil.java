package com.example.demo.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 客製驗證 Validation 工具類
 */
@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationUtil {

	/**
	 * 判斷該欄位的值是否重複，回傳包含錯誤訊息的 Map<data rowIndex, errorMessage>
	 *
	 * @param sheet            Excel 資料，List<Map<String, String>> 格式
	 * @param mappingFieldName 欲檢查重複的欄位名稱
	 * @return Map<data rowIndex, errorMessage>
	 */
	public static Map<Integer, String> validateDuplicate(List<Map<String, String>> sheet, String mappingFieldName) {
		Map<Integer, String> errorMap = new LinkedHashMap<>(); // 保持輸入順序
		Map<String, Integer> seenValues = new HashMap<>(); // 紀錄首次出現的值及其 rowIndex
		Set<String> recordedDuplicates = new HashSet<>(); // 紀錄已經加入 errorMap 的值

		for (int rowIndex = 0; rowIndex < sheet.size(); rowIndex++) {
			Map<String, String> row = sheet.get(rowIndex);
			String value = row.get(mappingFieldName);
			if (value != null) {
				if (seenValues.containsKey(value)) {
					// 第一次發現重複，將首次出現的索引也加入錯誤清單
					if (!recordedDuplicates.contains(value)) {
						int firstIndex = seenValues.get(value);
						errorMap.put(firstIndex + 1, String.format("欄位 %s 的值 '%s' 重複",
								mappingFieldName, value, firstIndex + 1));
						recordedDuplicates.add(value); // 確保首筆數據只被記錄一次
					}
					// 當前索引也加入錯誤清單
					errorMap.put(rowIndex + 1, String.format("欄位 %s 的值 '%s' 重複", mappingFieldName,
							value, seenValues.get(value) + 1));
				} else {
					// 記錄該值首次出現的索引
					seenValues.put(value, rowIndex);
				}
			}
		}
		return errorMap;
	}

	/**
	 * 判斷是否為數值
	 * 
	 * @param str
	 */
	public static boolean isNumeric(String str) {
		Pattern pattern = Pattern.compile("^-?[\\d]+(\\.[\\d]+)?$");
		Matcher isNum = pattern.matcher(str);
		if (!isNum.matches()) {
			return false;
		}
		return true;
	}

	public static boolean isNotNumeric(String str) {
		return !isNumeric(str);
	}

	public static boolean isNotNumericWithComma(String str) {
		return !isNumeric(str.replace(",", ""));
	}

	/**
	 * 判斷是否為整數
	 */
	public static boolean isInteger(String data) {
		try {
			Integer.parseInt(data);
			return false;
		} catch (NumberFormatException e) {
			return true;
		}
	}
}
