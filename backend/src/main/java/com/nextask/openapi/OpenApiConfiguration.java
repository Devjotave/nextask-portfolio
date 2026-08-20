package com.nextask.openapi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfiguration {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI nextaskOpenApi() {
        SecurityScheme bearerAuth = new SecurityScheme()
                .name(BEARER_AUTH)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT returned by POST /api/auth/register or /api/auth/login");

        return new OpenAPI()
                .info(new Info()
                        .title("NexTask API")
                        .version("0.1.0")
                        .description("REST API for collaborative workspace, project and task management."))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, bearerAuth));
    }
}
