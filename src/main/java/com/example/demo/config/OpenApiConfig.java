package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

/**
 * OpenAPI 3 / Swagger API 文件配置 (OpenAPI Configuration)
 * <p>
 * <b>架構定位與職責：</b><br>
 * 本配置類別遵循架構規範置於配置層 ({@code config} 套件)，作為最依賴外部技術細節的組裝樞紐。<br>
 * 負責配置與初始化 OpenAPI 3 規格文件 (SpringDoc)，提供視覺化 Swagger UI 介面，
 * 供開發者與前端團隊查閱表現層 REST API 端點規格、參數載體 (Resource) 與錯誤回應結構。
 * </p>
 */
@Configuration
public class OpenApiConfig {

	/**
	 * 配置全域 OpenAPI 中繼資訊與文件定義
	 *
	 * @return OpenAPI 規格實例
	 */
	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("客製化驗證模組 API (Customized Validation Module API)")
						.version("v1.0.0")
						.description("基於六角形架構 (Hexagonal Architecture) 與 DDD 設計之 Excel 雙層防腐對映（工作表與欄位標題）及客製驗證核心 (SpEL) RESTful API。")
						.contact(new Contact()
								.name("Architecture Team")
								.email("dev-team@example.com"))
						.license(new License()
								.name("Apache 2.0")
								.url("https://www.apache.org/licenses/LICENSE-2.0")));
	}
}
