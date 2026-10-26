/**
 * 客製驗證器核心運算套件 (CV Validator Core)
 * <p>
 * 核心元件 {@link com.example.demo.infra.cv.validator.CustomValidator} 負責：
 * <ul>
 *   <li>解析試算表結構並建立 SpEL 執行上下文 {@link org.springframework.expression.spel.support.StandardEvaluationContext}。</li>
 *   <li>向 SpEL 上下文自動註冊公用自訂驗證函數 (如 {@link com.example.demo.infra.util.ValidationUtil} 與 {@link com.example.demo.infra.util.VariableUtil})。</li>
 *   <li>依據 ROW (單列逐筆檢核) 與 SHEET (整表聚合比對) 規則評估表達式，產出結構化錯誤清單。</li>
 * </ul>
 * </p>
 */
package com.example.demo.infra.cv.validator;
