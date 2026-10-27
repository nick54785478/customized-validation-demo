package com.example.demo.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 表現層 Web 全域配置 (Web MVC Configuration)
 * <p>
 * 遵循架構規範集中存放於 {@code config/} 配置層。<br>
 * 負責擴充 Spring MVC 之 HTTP 訊息轉換器 (HttpMessageConverter)，
 * 讓 Jackson 轉換器額外支援 {@code application/octet-stream} 媒體型別，
 * 徹底解決 Swagger UI 或前端上傳 multipart/form-data 時，JSON 結構物件缺少 Content-Type 或預設標記為 octet-stream
 * 所導致之 HTTP 415 (Unsupported Media Type) 異常。
 * </p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		for (HttpMessageConverter<?> converter : converters) {
			if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
				List<MediaType> supportedMediaTypes = new ArrayList<>(jacksonConverter.getSupportedMediaTypes());
				if (!supportedMediaTypes.contains(MediaType.APPLICATION_OCTET_STREAM)) {
					supportedMediaTypes.add(MediaType.APPLICATION_OCTET_STREAM);
				}
				jacksonConverter.setSupportedMediaTypes(supportedMediaTypes);
			}
		}
	}
}
