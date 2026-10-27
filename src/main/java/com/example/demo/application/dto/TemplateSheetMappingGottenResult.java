package com.example.demo.application.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.example.demo.application.shared.vo.YesNo;

/**
 * 範本工作表對映查詢結果資料載體 (Template Sheet Mapping Gotten Result DTO)
 * <p>
 * 遵循 {@code N + Gotten + Result} 命名規範，以 Java 17 {@code record} 實作純資料載體 (Pure Data Carrier)。
 * 嚴守單一職責，內部嚴禁包含任何模型轉換邏輯。
 * </p>
 *
 * @param id           主鍵識別碼
 * @param templateName 範本名稱
 * @param sheetCode    內部工作表代碼
 * @param displayName  實體工作表顯示文字
 * @param sheetIndex   預期工作表索引
 * @param isRequired   是否為必備工作表
 * @param activeFlag   啟用旗標
 * @param createdTime  建立時間
 * @param updatedTime  更新時間
 */
public record TemplateSheetMappingGottenResult(
		Long id,
		String templateName,
		String sheetCode,
		String displayName,
		Integer sheetIndex,
		YesNo isRequired,
		YesNo activeFlag,
		LocalDateTime createdTime,
		LocalDateTime updatedTime) implements Serializable {
}
