package com.prospecta.discovery.infrastructure.apollo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "apollo")
public class ApolloProperties {

    private String baseUrl = "https://api.apollo.io";
    private String apiKey = "";
    private int timeoutSeconds = 15;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.equalsIgnoreCase("test") && !apiKey.contains("dummy");
    }
}
