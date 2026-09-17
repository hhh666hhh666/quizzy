package com.quizzy.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI quizzyOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Quizzy API")
                .version("v1")
                .description("答题程序接口文档，v1 仅支持选择题（单选 / 多选 / 判断）"));
    }
}
