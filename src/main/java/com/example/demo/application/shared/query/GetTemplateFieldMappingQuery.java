package com.example.demo.application.shared.query;

import java.io.Serializable;

/**
 * 取得範本欄位對映查詢 (Get Template Field Mapping Query)
 * <p>
 * 遵循 {@code V + N + Query} 命名規範，以 Java 17 {@code record} 實作之不可變查詢載體。
 * </p>
 *
 * @param id 主鍵識別碼
 */
public record GetTemplateFieldMappingQuery(Long id) implements Serializable {
}
