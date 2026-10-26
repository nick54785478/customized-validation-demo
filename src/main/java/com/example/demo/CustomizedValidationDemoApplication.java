package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 客製化驗證模組範例 Spring Boot 啟動類別 (Application Entry Point)
 * <p>
 * 提供本地端直接啟動與整合測試之用，載入 Spring 自動配置、JPA 倉儲與 AOP 切面。
 * </p>
 */
@SpringBootApplication
public class CustomizedValidationDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomizedValidationDemoApplication.class, args);
	}

}
