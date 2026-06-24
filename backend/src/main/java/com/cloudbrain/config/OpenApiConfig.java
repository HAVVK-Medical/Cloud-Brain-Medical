package com.cloudbrain.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cloudBrainMedicalOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cloud Brain Medical API")
                        .description("东软智慧云脑诊疗平台后端接口文档")
                        .version("v1.0.0")
                        .contact(new Contact().name("Cloud Brain Medical")));
    }

    @Bean
    public GroupedOpenApi apiGroup() {
        return GroupedOpenApi.builder()
                .group("cloud-brain-medical")
                .pathsToMatch("/api/**", "/ws/**")
                .build();
    }
}
