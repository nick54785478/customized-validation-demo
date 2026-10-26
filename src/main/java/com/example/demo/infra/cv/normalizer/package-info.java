/**
 * 試算表標題正規化與防腐模組套件 (Template Header Normalizer)
 * <p>
 * 位於基礎設施層客製驗證核心引擎 (Infrastructure CV Engine)。<br>
 * 負責在讀取 Excel 試算表後、送入規則評估引擎前，依據 {@code template_field_mapping} 進行：
 * <ul>
 *   <li><b>標題前置防呆校驗：</b> 檢查試算表是否缺漏必備之標題欄位。</li>
 *   <li><b>標題正規化：</b> 將外部客製標題（中文、英文等）轉換為內部統一之標準屬性代碼（CamelCase），扮演防腐層 (ACL) 角色。</li>
 * </ul>
 * </p>
 */
package com.example.demo.infra.cv.normalizer;
