package com.example.demo.application.shared.vo;

/**
 * 是否通用列舉值物件 (Yes / No Enum Value Object)
 * <p>
 * 位於應用層共享值物件套件 (Application Shared Value Objects)，以純 Java (Pure Java) 實作，
 * 嚴格遵循零技術框架侵入原則，不依賴 Lombok 或任何第三方函式庫。
 * 定義通用旗標常數（Y/N），供應用層及跨層資料處理使用。
 * </p>
 */
public enum YesNo {

	/**
	 * 是 (Yes)
	 */
	Y("Y"),

	/**
	 * 否 (No)
	 */
	N("N");

	private final String value;

	/**
	 * 建構子
	 *
	 * @param value 列舉常數字串值
	 */
	YesNo(String value) {
		this.value = value;
	}

	/**
	 * 取得常數字串值
	 *
	 * @return 列舉常數字串值 ("Y" 或 "N")
	 */
	public String getValue() {
		return value;
	}
}
