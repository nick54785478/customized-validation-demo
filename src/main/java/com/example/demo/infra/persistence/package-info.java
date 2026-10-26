/**
 * 基礎設施持久化套件 (Infrastructure Persistence)
 * <p>
 * 遵循六角形架構規範：
 * <ul>
 *   <li>依模組邊界集中管理所有與資料庫相關的存取邏輯。</li>
 *   <li>子套件細分為 {@code entity/} (ORM 實體) 與 {@code repository/} (Spring Data 倉儲介面)。</li>
 * </ul>
 * 專案退階後的驗證原則模型集中於此維護。
 * </p>
 */
package com.example.demo.infra.persistence;
