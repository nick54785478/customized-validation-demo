/**
 * 應用層連接埠套件 (Application Ports)
 * <p>
 * 遵循六角形架構規範，包含：
 * <ul>
 *   <li><b>Inbound Ports (UseCase)</b>：應用層對外公開的業務合約（例如驗證調度）。</li>
 *   <li><b>Outbound Ports (Port)</b>：應用層要求基礎設施層實作的對外輸出介面。</li>
 * </ul>
 * <b>規範警語：</b>Port 介面方法之傳參及回傳型別嚴禁使用任何技術類或環境相依之物件（如 {@code MultipartFile}、{@code HttpServletRequest} 等）。
 * </p>
 */
package com.example.demo.application.port;
