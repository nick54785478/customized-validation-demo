/**
 * 表現層資源模型套件 (Presentation Resources)
 * <p>
 * 依據規範分為：
 * <ul>
 *   <li>{@code in/}：接收外部請求的載體 (Request Data: {@code V + N + Resource})。</li>
 *   <li>{@code out/}：對外回傳的結果載體 (Response Data: {@code N + Ved + Resource})。</li>
 * </ul>
 * 嚴格作為純資料載體 (Pure Data Carrier)，禁止包含轉換與商業邏輯。
 * </p>
 */
package com.example.demo.iface.resource;
