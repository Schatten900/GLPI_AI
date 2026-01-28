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

    private OpenAIConfig openai = new OpenAIConfig();
    private SanitizerConfig sanitizer = new SanitizerConfig();
    private ClassificationConfig classification = new ClassificationConfig();

    @Data
    public static class OpenAIConfig {
        private String apiKey;
        private String endpoint = "https://api.openai.com/v1/chat/completions";
        private String model = "gpt-4o-mini";
        private Double temperature = 0.3;
        private Integer maxTokens = 500;
        private Integer timeout = 30000;
    }

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
}
