package com.quizzy.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI quizzyOpenApi(@Value("${quizzy.version:dev}") String version) {
        return new OpenAPI().info(new Info()
                .title("Quizzy API")
                // 这里曾经写死 "v1"——那是产品代次，不是版本号，占着 API 版本的位置会误导。
                // 现在由构建期注入的真版本号填充（ADR 0015）。
                .version(version)
                .description("答题程序接口文档，仅支持选择题（单选 / 多选 / 判断）"));
    }
}
