/**
 * 應用層 (Application Layer)
 * <p>
 * 依據六角形架構 (Hexagonal Architecture / Ports &amp; Adapters)，本層負責：
 * <ul>
 *   <li>調度業務使用案例 (Use Cases) 與流程控制。</li>
 *   <li>定義對外的業務合約介面 (Inbound Ports / Use Cases)。</li>
 *   <li>定義對外要求基礎設施提供的合約介面 (Outbound Ports)。</li>
 * </ul>
 * 嚴禁在此層直接依賴任何外部技術框架物件（如 ORM 實體、HTTP 協定物件等）。
 * </p>
 */
package com.example.demo.application;
