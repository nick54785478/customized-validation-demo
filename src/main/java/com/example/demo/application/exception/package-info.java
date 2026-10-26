/**
 * 應用層例外套件 (Application Exceptions)
 * <p>
 * 定義應用層 UseCase 與服務在執行業務驗證流程時拋出的業務例外：
 * <ul>
 *   <li>{@link com.example.demo.application.exception.ValidationException}：通用業務驗證例外。</li>
 *   <li>{@link com.example.demo.application.exception.ExcelValidationException}：Excel 試算表檢核失敗結構化例外。</li>
 * </ul>
 * 作為 UseCase 介面合約的一部分，純 Java 實作，零外部框架依賴。
 * </p>
 */
package com.example.demo.application.exception;
