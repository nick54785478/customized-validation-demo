/**
 * 表現層 / 介面層 (Presentation Layer / iface - Inbound Adapters)
 * <p>
 * 作為六角形架構的輸入適配器 (Inbound Adapters)，負責：
 * <ul>
 *   <li>接收外部客戶端之 HTTP / REST 請求或 AOP 切面攔截調用。</li>
 *   <li>將傳入的 Request Resource 轉換為應用層之 Command 或業務參數。</li>
 *   <li>呼叫應用層 UseCase 並將執行成果包裝為標準 Response 回傳。</li>
 * </ul>
 * 嚴守單一職責與防腐機制，Resource 嚴格作為純資料載體 (Pure Data Carrier)。
 * </p>
 */
package com.example.demo.iface;
