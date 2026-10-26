/**
 * 表現層 RESTful 控制器套件 (REST Controllers)
 * <p>
 * 提供對外之 HTTP API 端點，負責 HTTP 請求映射、Header / Content-Type 檢驗、
 * 委派 Assembler 進行資料轉換、調用應用層服務，並封裝回傳標準 {@code ResponseEntity} 與 HTTP 狀態碼。
 * </p>
 */
package com.example.demo.iface.rest;
