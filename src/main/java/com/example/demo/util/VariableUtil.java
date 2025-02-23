package com.example.demo.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 客製驗證 Variable 工具類
 */
@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VariableUtil {

	/**
	 * 根據 Sheet 建立 Set
	 * 
	 * @param sheet
	 * @param mappingFieldName
	 */
	public static Set<String> toSet(List<Map<String, String>> sheet, String mappingFieldName) {
		return sheet.stream().map(row -> row.get(mappingFieldName))
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * 建立 判斷用的 Set (先過濾出該欄位為 "Y" 的資料，再建立 Set)
	 * 
	 * @param sheet
	 * @param mappingFieldName
	 * @param adjMappingFieldName
	 */
	public static Set<String> toAdjSet(List<Map<String, String>> sheet, String mappingFieldName,
			String adjMappingFieldName) {
		return sheet.stream().filter(row -> StringUtils.equals("Y", row.get(adjMappingFieldName)))
				.map(row -> row.get(mappingFieldName)).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public static List<String> toList(List<Map<String, String>> sheet, String mappingFieldName) {
		return sheet.stream().map(row -> row.get(mappingFieldName)).collect(Collectors.toCollection(ArrayList::new));
	}

	public static <T> Set<T> diffSet(Set<T> s1, Set<T> s2) {
		return com.google.common.collect.Sets.difference(s1, s2);
	}

	public static <T> Set<T> intersectionSet(Set<T> s1, Set<T> s2) {
		return com.google.common.collect.Sets.intersection(s1, s2);
	}

	public static Map<Object, Object> toMap(List<Map<String, String>> sheet, String key, String value) {
		return sheet.stream().collect(
				Collectors.toMap(row -> row.get(key), row -> row.get(value), (r1, r2) -> r2, LinkedHashMap::new));
	}
	
}
