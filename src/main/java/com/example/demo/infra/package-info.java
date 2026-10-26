/**
 * 基礎設施層 (Infrastructure Layer / infra - Outbound Adapters)
 * <p>
 * 本層封裝所有與外部技術、資料庫、框架實作相關之細節：
 * <ul>
 *   <li><b>persistence</b>：依模組資料分類，包含 ORM 實體 (entity) 與 Spring Data 倉儲 (repository)。</li>
 *   <li><b>cv (Customized Validation)</b>：客製驗證核心引擎，負責 SpEL 動態評估與 Excel 解析。</li>
 *   <li><b>adapter</b>：所有基礎設施模組的匯聚點與對外出口，實作應用層 Outbound Port 合約。</li>
 * </ul>
 * 專案定位為驗證模組，原領域驗證原則已退階至本層持久化模組內。
 * </p>
 */
package com.example.demo.infra;
