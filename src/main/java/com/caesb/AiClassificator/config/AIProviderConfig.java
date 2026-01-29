package com.caesb.AiClassificator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracoes dos provedores de IA.
 * Valores podem ser definidos via application.yml ou variaveis de ambiente.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AIProviderConfig {

    private String defaultProvider = "azure-openai";
    private String defaultModel = "gpt-4o-mini";

    private SanitizerConfig sanitizer = new SanitizerConfig();
    private ClassificationConfig classification = new ClassificationConfig();
    private CacheConfig cache = new CacheConfig();

    @Data
    public static class SanitizerConfig {
        private Integer bodyMaxLength = 300;
        private Integer bodyMinLength = 200;
        private Boolean sanitizePii = true;
    }

    @Data
    public static class ClassificationConfig {
        private Double confidenceThreshold = 0.75;
        private String fallbackQueue = "Service Desk (1º Nivel)";
    }

    @Data
    public static class CacheConfig {
        private Integer ttlMinutes = 5;
        private Integer maxSize = 1000;
    }
}
