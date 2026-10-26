package com.example.demo.infra.util;

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
 * 客製驗證變數運算工具 (Variable Utility)
 * <p>
 * 位於基礎設施工具套件 (Infrastructure Utility)，提供註冊至 Spring SpEL 上下文執行的集合運算方法，
 * 支援由工作表資料動態投影產生 Set、List、過濾集合差集與交集運算。
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VariableUtil {

	/**
	 * 根據 Sheet 建立 Set
	 * 
	 * @param sheet            工作表資料
	 * @param mappingFieldName 欄位名稱
	 * @return 不重複集合
	 */
	public static Set<String> toSet(List<Map<String, String>> sheet, String mappingFieldName) {
		return sheet.stream()
				.map(row -> row.get(mappingFieldName))
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * 建立判斷用的 Set (先過濾出該欄位為 "Y" 的資料，再建立 Set)
	 * 
	 * @param sheet               工作表資料
	 * @param mappingFieldName    目標欄位名稱
	 * @param adjMappingFieldName 判斷旗標欄位名稱
	 * @return 過濾後的集合
	 */
	public static Set<String> toAdjSet(List<Map<String, String>> sheet, String mappingFieldName,
			String adjMappingFieldName) {
		return sheet.stream()
				.filter(row -> StringUtils.equals("Y", row.get(adjMappingFieldName)))
				.map(row -> row.get(mappingFieldName))
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * 轉換為 List
	 *
	 * @param sheet            工作表資料
	 * @param mappingFieldName 欄位名稱
	 * @return 清單
	 */
	public static List<String> toList(List<Map<String, String>> sheet, String mappingFieldName) {
		return sheet.stream()
				.map(row -> row.get(mappingFieldName))
				.collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * 計算集合差集 (s1 - s2)
	 *
	 * @param s1 集合 1
	 * @param s2 集合 2
	 * @return 差集
	 */
	public static <T> Set<T> diffSet(Set<T> s1, Set<T> s2) {
		return com.google.common.collect.Sets.difference(s1, s2);
	}

	/**
	 * 計算集合交集 (s1 ∩ s2)
	 *
	 * @param s1 集合 1
	 * @param s2 集合 2
	 * @return 交集
	 */
	public static <T> Set<T> intersectionSet(Set<T> s1, Set<T> s2) {
		return com.google.common.collect.Sets.intersection(s1, s2);
	}

	/**
	 * 轉換為 Map
	 *
	 * @param sheet 工作表資料
	 * @param key   鍵欄位
	 * @param value 值欄位
	 * @return 鍵值映射表
	 */
	public static Map<Object, Object> toMap(List<Map<String, String>> sheet, String key, String value) {
		return sheet.stream().collect(
				Collectors.toMap(row -> row.get(key), row -> row.get(value), (r1, r2) -> r2, LinkedHashMap::new));
	}
	
}
