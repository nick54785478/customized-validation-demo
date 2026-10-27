package com.example.demo.application.shared.command;

import java.io.Serializable;

/**
 * 刪除範本欄位對映指令 (Delete Template Field Mapping Command)
 * <p>
 * 遵循 {@code V + N + Command} 命名規範，以 Java 17 {@code record} 實作之不可變業務指令載體。
 * </p>
 *
 * @param id 主鍵識別碼
 */
public record DeleteTemplateFieldMappingCommand(Long id) implements Serializable {
}
