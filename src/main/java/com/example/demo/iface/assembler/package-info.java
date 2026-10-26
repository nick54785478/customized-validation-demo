/**
 * 表現層資料組裝防腐套件 (Presentation Assemblers / Anti-Corruption Layer)
 * <p>
 * 依據 Clean Architecture 規範：
 * 獨立於 {@code resource/} 之外，負責執行 Request/Response Resource 與 Command/DTO 之間的資料互轉，
 * 防止外部傳輸物件污染應用核心。
 * </p>
 */
package com.example.demo.iface.assembler;
