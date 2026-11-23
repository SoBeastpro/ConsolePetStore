package ru.mirea.petstore.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Конфигурирует документацию REST API. */
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI petStoreOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Pet Store REST API").version("1.0.0")
                        .description("REST API питомника: заявки на резерв животных и клиенты"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
