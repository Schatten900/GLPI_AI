package com.caesb.AiClassificator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuracoes especificas do Azure OpenAI.
 * Suporta multiplos deployments (modelos) em um unico recurso Azure.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.azure-openai")
public class AzureOpenAIConfig {

    /**
     * Se Azure OpenAI esta habilitado.
     */
    private boolean enabled = false;

    /**
     * Nome do recurso Azure OpenAI (parte da URL).
     */
    private String resourceName;

    /**
     * API Key do Azure OpenAI.
     */
    private String apiKey;

    /**
     * Versao da API Azure OpenAI.
     */
    private String apiVersion = "2024-02-01";

    /**
     * Timeout em milissegundos.
     */
    private Integer timeout = 30000;

    /**
     * Temperatura padrao.
     */
    private Double defaultTemperature = 0.3;

    /**
     * Max tokens padrao.
     */
    private Integer defaultMaxTokens = 500;

    /**
     * Deployments configurados (key = modelId, value = config do deployment).
     */
    private Map<String, DeploymentConfig> deployments = new HashMap<>();

    /**
     * Configuracao de um deployment individual.
     */
    @Data
    public static class DeploymentConfig {
        /**
         * Nome do deployment no Azure.
         */
        private String deploymentName;

        /**
         * Nome de exibicao para UI.
         */
        private String displayName;

        /**
         * Descricao do modelo.
         */
        private String description;

        /**
         * Se o deployment esta habilitado.
         */
        private boolean enabled = true;

        /**
         * Temperatura especifica deste modelo (opcional).
         */
        private Double temperature;

        /**
         * Max tokens especifico deste modelo (opcional).
         */
        private Integer maxTokens;
    }

    /**
     * Constroi a URL do endpoint para um deployment especifico.
     */
    public String buildEndpointUrl(String deploymentName) {
        return String.format(
                "https://%s.openai.azure.com/openai/deployments/%s/chat/completions?api-version=%s",
                resourceName,
                deploymentName,
                apiVersion
        );
    }

    /**
     * Verifica se a configuracao Azure esta completa.
     */
    public boolean isConfigured() {
        return enabled
                && resourceName != null && !resourceName.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && !deployments.isEmpty();
    }
}
