/**
 * 驗證上下文模型套件 (CV Evaluation Context)
 * <p>
 * 維護動態 SpEL 表達式求值時的執行上下文環境：
 * <ul>
 *   <li>{@link com.example.demo.infra.cv.shared.context.ContextRoot}：承載當前整個試算表多 Sheet 資料、參數表 (params) 與當前列資料。</li>
 *   <li>{@link com.example.demo.infra.cv.shared.context.SheetContextRoot}：單一 Sheet 的上下文資料結構。</li>
 * </ul>
 * </p>
 */
package com.example.demo.infra.cv.shared.context;
