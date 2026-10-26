package com.example.demo.application.shared.vo;

import java.io.Serializable;

/**
 * 範本檢核錯誤值物件 (Validation Error Value Object)
 * <p>
 * 位於應用層共享值物件套件 (Application Shared Value Objects)，以 Java 17 {@code record} 實作。
 * 作為不可變的錯誤事實快照 (Immutable Error Fact)，封裝規則代碼、錯誤列號與詳細錯誤說明，
 * 自帶屬性等價性比對 (Value Equality) 與唯讀封裝，完全符合 DDD 值物件規範。
 * </p>
 *
 * @param rule     觸發之檢核規則代碼 (例如: ENFORCE_ROW_VALIDATION)
 * @param rowIndex 發生錯誤的資料列索引 (1-based)
 * @param message  格式化之詳細檢核錯誤訊息
 */
public record ValidationError(String rule, int rowIndex, String message) implements Serializable {

	/**
	 * 便捷建構子：僅提供錯誤訊息（適用於無特定規則代碼或整表錯誤情境）
	 *
	 * @param message 詳細錯誤訊息
	 */
	public ValidationError(String message) {
		this(null, 0, message);
	}

	/**
	 * 取得錯誤訊息（JavaBean 兼容方法）
	 *
	 * @return 錯誤訊息
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * 取得規則名稱（JavaBean 兼容方法）
	 *
	 * @return 規則名稱
	 */
	public String getRule() {
		return rule;
	}

	/**
	 * 取得資料列索引（JavaBean 兼容方法）
	 *
	 * @return 資料列索引
	 */
	public int getRowIndex() {
		return rowIndex;
	}
}

