package com.caesb.AiClassificator.config;

import com.caesb.AiClassificator.client.AIProviderFactory;
import com.caesb.AiClassificator.client.AIProviderRegistry;
import com.caesb.AiClassificator.model.AIResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health Indicator customizado para verificar conectividade com Azure OpenAI.
 * Exibido no /actuator/health quando autorizado.
 */
@Slf4j
@Component("azureOpenAI")
@RequiredArgsConstructor
public class AzureHealthIndicator implements HealthIndicator {

    private final AzureOpenAIConfig azureConfig;
    private final AIProviderRegistry registry;
    private final AIProviderFactory factory;

    // Cache do ultimo status para evitar chamadas excessivas
    private volatile Health cachedHealth;
    private volatile long cacheTimestamp;
    private static final long CACHE_TTL_MS = 60_000; // 1 minuto

    @Override
    public Health health() {
        // Retorna cache se valido
        if (cachedHealth != null && (System.currentTimeMillis() - cacheTimestamp) < CACHE_TTL_MS) {
            return cachedHealth;
        }

        try {
            cachedHealth = checkAzureHealth();
            cacheTimestamp = System.currentTimeMillis();
            return cachedHealth;
        } catch (Exception e) {
            log.warn("Erro ao verificar saude do Azure: {}", e.getMessage());
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private Health checkAzureHealth() {
        // Verifica se Azure esta configurado
        if (!azureConfig.isEnabled()) {
            return Health.down()
                    .withDetail("status", "Azure OpenAI desabilitado")
                    .build();
        }

        if (!azureConfig.isConfigured()) {
            return Health.down()
                    .withDetail("status", "Azure OpenAI nao configurado")
                    .withDetail("resourceName", azureConfig.getResourceName() != null)
                    .withDetail("apiKey", azureConfig.getApiKey() != null && !azureConfig.getApiKey().isBlank())
                    .build();
        }

        // Verifica se tem providers registrados
        if (!registry.isProviderAvailable("azure-openai")) {
            return Health.down()
                    .withDetail("status", "Nenhum modelo Azure disponivel")
                    .build();
        }

        // Tenta conexao com modelo default
        String defaultModel = registry.getDefaultModel();
        AIResponse testResponse = factory.testConnection("azure-openai", defaultModel);

        if (testResponse.isSuccess()) {
            return Health.up()
                    .withDetail("provider", "azure-openai")
                    .withDetail("resource", azureConfig.getResourceName())
                    .withDetail("defaultModel", defaultModel)
                    .withDetail("modelsAvailable", registry.getTotalModelsCount())
                    .withDetail("latencyMs", testResponse.getLatencyMs())
                    .build();
        } else {
            return Health.down()
                    .withDetail("provider", "azure-openai")
                    .withDetail("defaultModel", defaultModel)
                    .withDetail("errorCode", testResponse.getErrorCode())
                    .withDetail("errorMessage", testResponse.getErrorMessage())
                    .build();
        }
    }
}
