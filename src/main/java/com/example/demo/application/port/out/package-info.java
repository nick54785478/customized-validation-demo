/**
 * 應用層輸出連接埠套件 (Outbound Ports)
 * <p>
 * 依據六角形架構與命名規範：
 * <ul>
 *   <li>對外要求的基礎設施合約介面一律以 {@code Port} 結尾（如 {@link com.example.demo.application.port.out.ExcelValidatorPort}）。</li>
 *   <li>方法之傳入與回傳型別嚴格使用純 Java 核心型別與領域資料載體，嚴禁接發 ORM 實體或 HTTP/Web 協定技術物件。</li>
 * </ul>
 * </p>
 */
package com.example.demo.application.port.out;
