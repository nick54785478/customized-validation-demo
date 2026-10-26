/**
 * 表現層全域通知與例外攔截套件 (Presentation Controller Advice)
 * <p>
 * 遵循六角形架構規範：
 * 作為表現層輸入適配器，攔截由內層（應用層）拋出的業務例外，統一封裝轉譯為 HTTP 狀態碼與 JSON 回應酬載。
 * </p>
 */
package com.example.demo.iface.advice;
