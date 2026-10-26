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
import com.example.demo.infra.persistence.entity.TemplateFieldMapping;

import lombok.extern.slf4j.Slf4j;

/**
 * Excel 標題正規化防腐器 (Template Header Normalizer)
 * <p>
 * <b>架構定位與職責：</b><br>
 * 本元件位於基礎設施層客製驗證核心引擎之正規化模組（{@code infra.cv.normalizer}），
 * 作為外部試算表實體結構與內部領域/應用層模型之間的<b>防腐層 (Anti-Corruption Layer - ACL)</b>。<br>
 * 負責依據 {@link TemplateFieldMapping} 欄位定義執行：
 * <ol>
 *   <li><b>標題結構前置防呆校驗 (Header Verification)：</b>
 *       在進入單列或整表資料內容計算前，先檢查試算表是否缺漏必備之標題欄位（{@code isRequired == YesNo.Y}）；
 *       若有缺漏立即拋出 {@link ExcelValidationException} 中止無意義的後續內容運算。</li>
 *   <li><b>屬性代碼正規化 (Key Normalization)：</b>
 *       將資料列 Map 中的實體標題鍵值（如「姓名」、「身分證字號」或「NATION_ID」），
 *       動態轉換為系統內部標準統一的屬性代碼（如 {@code name}, {@code nationId}），
 *       使後續 {@code CustomValidator} 規則檢核與 {@code BaseDataTransformer} DTO 映射零命名阻抗。</li>
 * </ol>
 * </p>
 */
@Slf4j
@Component
public class TemplateHeaderNormalizer {

	/**
	 * 執行多工作表資料之標題校驗與鍵值正規化轉換
	 *
	 * @param templateName 範本名稱 (例如: USER_PROFILE)
	 * @param rawSheetMap  ExcelUtil 原始解析出之工作表資料集 Map<SheetName, List<RowMap>> (Key 為 Excel 原始標題)
	 * @param mappings     該範本設定之欄位對映定義清單 (若為空則採向下相容原始模式直接回傳)
	 * @return 鍵值正規化後之工作表資料集 (Key 已轉換為內部標準 fieldCode)
	 * @throws ExcelValidationException 當缺少必要欄位標題時拋出結構化檢核例外
	 */
	public Map<String, List<Map<String, String>>> normalize(
			String templateName,
			Map<String, List<Map<String, String>>> rawSheetMap,
			List<TemplateFieldMapping> mappings) {

		// 若未設定任何欄位對映，維持向下相容機制（使用 Excel 原始標題作為 Key）
		if (mappings == null || mappings.isEmpty()) {
			log.debug("範本 [{}] 未設定欄位對映表，採用原始標題鍵值模式", templateName);
			return rawSheetMap;
		}

		// 依據工作表名稱分群對映定義
		Map<String, List<TemplateFieldMapping>> mappingBySheet = mappings.stream()
				.collect(Collectors.groupingBy(TemplateFieldMapping::getTemplateSheetName));

		Map<String, List<Map<String, String>>> normalizedSheetMap = new LinkedHashMap<>();
		List<ValidationError> headerErrors = new ArrayList<>();

		// 逐一處理各工作表
		rawSheetMap.forEach((sheetName, rawRowList) -> {
			List<TemplateFieldMapping> sheetMappings = mappingBySheet.get(sheetName);

			// 若該 Sheet 未配置對映規則，維持原樣
			if (sheetMappings == null || sheetMappings.isEmpty()) {
				normalizedSheetMap.put(sheetName, rawRowList);
				return;
			}

			// 建立 實體標題 (trimmed) 與屬性代碼 -> 內部代碼 (fieldCode) 的快照字典
			Map<String, String> displayToFieldCodeMap = new LinkedHashMap<>();
			for (TemplateFieldMapping m : sheetMappings) {
				String fieldCode = m.getFieldCode();
				displayToFieldCodeMap.put(StringUtils.trimToEmpty(m.getDisplayName()), fieldCode);
				// 同時註冊 fieldCode 本身及其大小寫變體，支援向後相容技術代碼標題 (例如 NAME 與 name)
				displayToFieldCodeMap.putIfAbsent(fieldCode, fieldCode);
				displayToFieldCodeMap.putIfAbsent(fieldCode.toUpperCase(), fieldCode);
				displayToFieldCodeMap.putIfAbsent(fieldCode.toLowerCase(), fieldCode);
			}

			// 步驟 1: 標題前置防呆校驗 (Header Verification)
			validateRequiredHeaders(sheetName, rawRowList, sheetMappings, headerErrors);

			// 步驟 2: 逐列鍵值替換 (Key Normalization)
			List<Map<String, String>> normalizedRowList = new ArrayList<>();
			for (Map<String, String> rawRow : rawRowList) {
				Map<String, String> normalizedRow = new LinkedHashMap<>();
				rawRow.forEach((rawHeader, cellValue) -> {
					String cleanHeader = StringUtils.trimToEmpty(rawHeader);
					// 若對映表中存在該標題，使用內部 fieldCode；否則保留原始標題作為 fallback
					String fieldCode = displayToFieldCodeMap.getOrDefault(cleanHeader, cleanHeader);
					normalizedRow.put(fieldCode, cellValue);
				});
				normalizedRowList.add(normalizedRow);
			}

			normalizedSheetMap.put(sheetName, normalizedRowList);
		});

		// 若發現任何缺少必備標題之錯誤，立即拋出例外阻斷後續流程
		if (!headerErrors.isEmpty()) {
			log.warn("範本 [{}] Excel 標題校驗失敗，缺少必要標題數量: {}", templateName, headerErrors.size());
			throw new ExcelValidationException("HEADER_VALIDATION_FAILED", headerErrors);
		}

		return normalizedSheetMap;
	}

	/**
	 * 檢查試算表是否具備所有標註為必備之標題欄位
	 *
	 * @param sheetName     工作表名稱
	 * @param rawRowList    原始資料列集合
	 * @param sheetMappings 該工作表之定義清單
	 * @param headerErrors  錯誤收集容器
	 */
	private void validateRequiredHeaders(
			String sheetName,
			List<Map<String, String>> rawRowList,
			List<TemplateFieldMapping> sheetMappings,
			List<ValidationError> headerErrors) {

		// 取得試算表中實際出現的所有標題集合
		Set<String> uploadedHeaders = rawRowList.isEmpty()
				? Collections.emptySet()
				: rawRowList.get(0).keySet().stream()
						.map(StringUtils::trimToEmpty)
						.collect(Collectors.toSet());

		// 過濾出所有必填標題定義
		List<TemplateFieldMapping> requiredMappings = sheetMappings.stream()
				.filter(m -> m.getIsRequired() == YesNo.Y)
				.toList();

		for (TemplateFieldMapping required : requiredMappings) {
			String expectedDisplayName = StringUtils.trimToEmpty(required.getDisplayName());
			String fieldCode = StringUtils.trimToEmpty(required.getFieldCode());
			boolean headerPresent = uploadedHeaders.contains(expectedDisplayName)
					|| uploadedHeaders.contains(fieldCode)
					|| uploadedHeaders.stream().anyMatch(h -> h.equalsIgnoreCase(fieldCode) || h.equalsIgnoreCase(expectedDisplayName));

			if (!headerPresent) {
				log.warn("工作表 [{}] 缺少必要標題: 【{}】 (預期代碼: {})",
						sheetName, expectedDisplayName, required.getFieldCode());
				headerErrors.add(new ValidationError(
						"REQUIRED_HEADER_MISSING",
						1, // 標題列固定為第 1 列
						"SheetName : " + sheetName + ", 缺少必要欄位標題：【" + expectedDisplayName + "】"
				));
			}
		}
	}
}
