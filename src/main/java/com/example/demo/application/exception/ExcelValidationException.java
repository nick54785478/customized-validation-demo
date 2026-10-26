package com.example.demo.application.exception;

import java.util.List;

import com.example.demo.application.shared.vo.ValidationError;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Excel 範本校驗失敗例外 (Excel Validation Exception)
 * <p>
 * 位於應用層例外 (Application Exception)，代表在執行 Excel 試算表檢核流程時，
 * 經由客製驗證引擎評估發現一筆或多筆不符合業務規則之資料時所拋出。
 * 內部包含不可變的詳細 {@link ValidationError} 值物件清單。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ExcelValidationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 錯誤代碼 (例如: "VALIDATED_FAILED")
	 */
	private String code;

	/**
	 * 詳細檢核錯誤值物件清單
	 */
	private List<ValidationError> errors;

	/**
	 * 兼容性方法：取得錯誤清單
	 *
	 * @return 錯誤值物件清單
	 */
	public List<ValidationError> getVepList() {
		return errors;
	}
}
