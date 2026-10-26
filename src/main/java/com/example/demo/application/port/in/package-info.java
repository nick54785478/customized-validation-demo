/**
 * 應用層輸入連接埠套件 (Inbound Ports / Use Cases)
 * <p>
 * 依據六角形架構與命名規範：
 * <ul>
 *   <li>對外開放的業務合約一律以 {@code UseCase} 結尾（如 {@link com.example.demo.application.port.in.ValidateExcelUseCase}）。</li>
 *   <li>方法之傳入與回傳型別嚴格使用純 Java 核心型別、Command 或領域資料載體，嚴禁接發環境或技術類物件。</li>
 * </ul>
 * </p>
 */
package com.example.demo.application.port.in;
