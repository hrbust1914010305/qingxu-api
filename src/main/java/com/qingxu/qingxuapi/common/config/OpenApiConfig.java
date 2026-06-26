package com.qingxu.qingxuapi.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI qingxuOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Qingxu API")
                        .description("Qingxu backend APIs")
                        .version("0.0.1"));
    }
}
