package com.ailearn.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档配置(springdoc-openapi,访问 /swagger-ui.html)。
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI aiLearningOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("基于AI大模型的智能学习系统 API")
                        .version("1.0.0")
                        .description("智能出题 / 智能刷题 / 智能考试三大场景;Redis 缓存;MinIO 资源存储")
                        .contact(new Contact().name("ai-learning")))
                .components(new Components().addSecuritySchemes(SCHEME_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME));
    }
}
