/**
 * 應用層共享實質物件套件 (Application Shared Value Objects - VO)
 * <p>
 * 遵循 DDD (Domain-Driven Design) 規範：
 * 存放不可變之值物件 (Value Objects)，無獨立生命週期或實體 ID，其等價性完全取決於內部屬性值。
 * 典型代表如 {@link com.example.demo.application.shared.vo.ValidationError} 與 {@link com.example.demo.application.shared.vo.YesNo}。
 * </p>
 */
package com.example.demo.application.shared.vo;
