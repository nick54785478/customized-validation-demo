package com.example.demo.infra.cv.normalizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.example.demo.application.exception.ExcelValidationException;
import com.example.demo.application.shared.vo.ValidationError;
import com.example.demo.application.shared.vo.YesNo;
import com.example.demo.infra.persistence.entity.TemplateSheetMapping;

import lombok.extern.slf4j.Slf4j;

/**
 * Excel 工作表名稱正規化防腐器 (Template Sheet Normalizer - Level 1 ACL)
 * <p>
 * <b>架構定位與職責：</b><br>
 * 本元件隸屬於基礎設施層客製驗證核心引擎（{@code infra.cv.normalizer}），
 * 作為外部試算表實體工作表（Sheet）結構進入內部業務檢核前的<b>第一層防腐層 (Sheet-Level Anti-Corruption Layer)</b>。<br>
 * 負責依據 {@link TemplateSheetMapping} 設定執行：
 * <ol>
 *   <li><b>工作表前置防呆校驗 (Sheet Verification)：</b>
 *       在解析內容前，核對試算表是否缺漏標記為必備的工作表（{@code isRequired == YesNo.Y}）。
 *       若缺失則立即拋出 {@link ExcelValidationException} 中斷後續不必要的內容處理。</li>
 *   <li><b>工作表名稱正規化 (Sheet Code Normalization)：</b>
 *       將上傳 Excel 的外部頁籤名稱（如「使用者清單」、「部門資料」）
 *       正規化轉換為系統內部統一的工作表代碼（如 {@code USER}, {@code DEPT}），
 *       使下游的欄位正規化 {@link TemplateHeaderNormalizer} 與驗證核心得以統一基於標準代碼運作。</li>
 * </ol>
 * </p>
 */
@Slf4j
@Component
public class TemplateSheetNormalizer {

	/**
	 * 執行工作表層級之前置防呆校驗與工作表代碼正規化轉換
	 *
	 * @param templateName  範本識別名稱 (例如: "USER_PROFILE")
	 * @param rawSheetMap   由 Excel 二進位串流解析所得之原始多工作表資料集 Map&lt;ActualSheetName, List&lt;RowMap&gt;&gt;
	 * @param sheetMappings 該範本設定於資料庫之工作表對映清單
	 * @return 正規化後的資料集 Map&lt;NormalizedSheetCode, List&lt;RowMap&gt;&gt;
	 * @throws ExcelValidationException 當試算表缺少必要工作表時拋出結構化檢核例外
	 */
	public Map<String, List<Map<String, String>>> normalize(
			String templateName,
			Map<String, List<Map<String, String>>> rawSheetMap,
			List<TemplateSheetMapping> sheetMappings) {

		// 若未設定任何工作表對映，維持向下相容機制（使用 Excel 原始工作表名稱）
		if (sheetMappings == null || sheetMappings.isEmpty()) {
			log.debug("範本 [{}] 未設定工作表對映表，採用原始工作表名稱模式", templateName);
			return rawSheetMap;
		}

		// 建立 實體工作表名稱 (trimmed) 與 工作表代碼 -> 內部代碼 (sheetCode) 的快照字典
		Map<String, String> displayToSheetCodeMap = new LinkedHashMap<>();
		for (TemplateSheetMapping m : sheetMappings) {
			String sheetCode = m.getSheetCode();
			displayToSheetCodeMap.put(StringUtils.trimToEmpty(m.getDisplayName()), sheetCode);
			// 支援直接使用 sheetCode 本身及其大小寫變體
			displayToSheetCodeMap.putIfAbsent(sheetCode, sheetCode);
			displayToSheetCodeMap.putIfAbsent(sheetCode.toUpperCase(), sheetCode);
			displayToSheetCodeMap.putIfAbsent(sheetCode.toLowerCase(), sheetCode);
		}

		// 步驟 1: 工作表前置防呆校驗 (Sheet Verification)
		List<ValidationError> sheetErrors = new ArrayList<>();
		validateRequiredSheets(templateName, rawSheetMap.keySet(), sheetMappings, sheetErrors);

		if (!sheetErrors.isEmpty()) {
			log.warn("範本 [{}] Excel 工作表校驗失敗，缺少必要工作表數量: {}", templateName, sheetErrors.size());
			throw new ExcelValidationException("SHEET_VALIDATION_FAILED", sheetErrors);
		}

		// 步驟 2: 工作表名稱正規化替換 (Sheet Normalization)
		Map<String, List<Map<String, String>>> normalizedSheetMap = new LinkedHashMap<>();
		rawSheetMap.forEach((rawSheetName, rowList) -> {
			String cleanName = StringUtils.trimToEmpty(rawSheetName);
			// 若存在對映則轉換為內部 sheetCode；否則保留原始名稱作為 fallback
			String targetSheetCode = displayToSheetCodeMap.getOrDefault(cleanName, cleanName);
			normalizedSheetMap.put(targetSheetCode, rowList);
		});

		return normalizedSheetMap;
	}

	/**
	 * 校驗試算表中是否包含所有必要之工作表
	 *
	 * @param templateName      範本名稱
	 * @param uploadedSheetNames 試算表中實際出現的所有工作表名稱集合
	 * @param sheetMappings     該範本定義之工作表對映清單
	 * @param sheetErrors       錯誤收集容器
	 */
	private void validateRequiredSheets(
			String templateName,
			Set<String> uploadedSheetNames,
			List<TemplateSheetMapping> sheetMappings,
			List<ValidationError> sheetErrors) {

		Set<String> cleanUploadedNames = uploadedSheetNames == null
				? Collections.emptySet()
				: uploadedSheetNames.stream()
						.map(StringUtils::trimToEmpty)
						.collect(Collectors.toSet());

		// 篩選所有標記為必備的工作表
		List<TemplateSheetMapping> requiredMappings = sheetMappings.stream()
				.filter(m -> m.getIsRequired() == YesNo.Y)
				.toList();

		for (TemplateSheetMapping required : requiredMappings) {
			String expectedDisplayName = StringUtils.trimToEmpty(required.getDisplayName());
			String sheetCode = StringUtils.trimToEmpty(required.getSheetCode());

			// 允許匹配 display_name (如「使用者清單」) 或 sheet_code (如「USER」)
			boolean sheetPresent = cleanUploadedNames.contains(expectedDisplayName)
					|| cleanUploadedNames.contains(sheetCode)
					|| cleanUploadedNames.stream().anyMatch(name -> name.equalsIgnoreCase(sheetCode) || name.equalsIgnoreCase(expectedDisplayName));

			if (!sheetPresent) {
				log.warn("範本 [{}] 缺少必要工作表: 【{}】 (預期代碼: {})", templateName, expectedDisplayName, sheetCode);
				sheetErrors.add(new ValidationError(
						"REQUIRED_SHEET_MISSING",
						0, // 0 代表工作表層級錯誤
						"缺少必要工作表：【" + expectedDisplayName + "】(預期工作表代碼: " + sheetCode + ")"
				));
			}
		}
	}
}
