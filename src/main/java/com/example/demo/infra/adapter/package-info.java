/**
 * 基礎設施適配器套件 (Infrastructure Outbound Adapters)
 * <p>
 * 作為六角形架構中所有基礎設施模組的匯聚點與對外出口。
 * 實作位於應用層之連接埠介面 (如 {@link com.example.demo.application.port.out.ExcelValidatorPort})，
 * 由適配器協調整合持久化倉儲 (Persistence Repository) 與驗證核心引擎 (CV Validator)。
 * 類別命名一律以 {@code Adapter} 結尾。
 * </p>
 */
package com.example.demo.infra.adapter;
