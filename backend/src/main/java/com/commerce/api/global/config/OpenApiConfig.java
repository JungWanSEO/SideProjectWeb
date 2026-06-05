package com.commerce.api.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI(Swagger) 문서 기본 정보 설정.
 * 문서: http://localhost:8080/swagger-ui.html  (스펙: /v3/api-docs)
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI commerceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("commerce-api")
                        .description("패션 커머스 백엔드 API 문서 (member / product / order / cart)")
                        .version("v0.0.1"));
    }
}