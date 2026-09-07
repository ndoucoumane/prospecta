package com.prospecta.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Keycloak-JWT";

    @Bean
    public OpenAPI prospectaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Prospecta SaaS API")
                        .description("B2B Sales Automation & Lead Intelligence Platform Backend for Senegal & Francophone Africa")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Prospecta Team")
                                .email("contact@prospecta.sn")
                                .url("https://prospecta.sn"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://prospecta.sn/terms")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Authenticate using a valid Keycloak JWT token.")));
    }
}
