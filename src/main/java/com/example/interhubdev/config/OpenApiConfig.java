package com.example.interhubdev.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * OpenAPI/Swagger configuration.
 * 
 * <p>Provides API documentation with JWT authentication support.</p>
 * 
 * <p>Swagger UI: {@code /swagger-ui.html} (or {@code /swagger-ui} → redirect).</p>
 * <p>OpenAPI JSON: {@code /api-docs} (see {@code springdoc.api-docs.path}).</p>
 */
@Configuration
public class OpenApiConfig implements WebMvcConfigurer {

    /**
     * Many guides link to {@code /swagger-ui} without {@code .html}; springdoc only serves
     * {@code /swagger-ui.html} and static assets under {@code /swagger-ui/...}, so bare
     * {@code /swagger-ui} would 404 without this redirect.
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/swagger-ui", "/swagger-ui.html");
        registry.addRedirectViewController("/swagger-ui/", "/swagger-ui.html");
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("InterHubDev API")
                .description("API для платформы международных студентов")
                .version("1.0.0")
                .contact(new Contact()
                    .name("InterHubDev Team")
                    .email("support@interhubdev.com"))
                .license(new License()
                    .name("MIT")
                    .url("https://opensource.org/licenses/MIT")))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                                "JWT доступа: либо HttpOnly cookie (веб), либо заголовок Authorization: Bearer <token>. "
                                        + "При логине с заголовком X-Auth-Tokens: json токены также приходят в теле ответа. "
                                        + "В Swagger укажите только значение токена (без слова Bearer).")));
    }
}
