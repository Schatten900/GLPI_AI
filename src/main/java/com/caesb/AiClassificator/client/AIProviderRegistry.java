package com.caesb.AiClassificator.client;

import com.caesb.AiClassificator.config.AIProviderConfig;
import com.caesb.AiClassificator.config.AzureOpenAIConfig;
import com.caesb.AiClassificator.model.AIDeployment;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Registry de providers e modelos de IA disponiveis.
 * Mantem lista de todos os providers/modelos configurados e habilitados.
 * Atualmente suporta apenas Azure OpenAI.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AIProviderRegistry {

    private final AzureOpenAIConfig azureConfig;
    private final AIProviderConfig providerConfig;

    @Getter
    private final Map<String, List<AIDeployment>> providerDeployments = new LinkedHashMap<>();

    @Getter
    private String defaultProvider;

    @Getter
    private String defaultModel;

    @PostConstruct
    public void init() {
        loadDefaultSettings();
        loadAzureDeployments();

        log.info("AIProviderRegistry inicializado - providers: {}, default: {}/{}",
                providerDeployments.keySet(), defaultProvider, defaultModel);
    }

    private void loadDefaultSettings() {
        // Defaults configurados via application.yml
        this.defaultProvider = providerConfig.getDefaultProvider();
        this.defaultModel = providerConfig.getDefaultModel();

        // Fallbacks se nao configurado
        if (defaultProvider == null || defaultProvider.isBlank()) {
            defaultProvider = "azure-openai";
        }
        if (defaultModel == null || defaultModel.isBlank()) {
            defaultModel = "gpt-4o-mini";
        }
    }

    private void loadAzureDeployments() {
        if (!azureConfig.isEnabled()) {
            log.warn("Azure OpenAI desabilitado - nenhum provider disponivel");
            return;
        }

        List<AIDeployment> deployments = new ArrayList<>();

        azureConfig.getDeployments().forEach((modelId, config) -> {
            if (config.isEnabled()) {
                AIDeployment deployment = AIDeployment.builder()
                        .modelId(modelId)
                        .deploymentName(config.getDeploymentName())
                        .displayName(config.getDisplayName() != null ? config.getDisplayName() : modelId)
                        .description(config.getDescription())
                        .enabled(true)
                        .defaultTemperature(config.getTemperature() != null
                                ? config.getTemperature() : azureConfig.getDefaultTemperature())
                        .defaultMaxTokens(config.getMaxTokens() != null
                                ? config.getMaxTokens() : azureConfig.getDefaultMaxTokens())
                        .build();
                deployments.add(deployment);
                log.debug("Registrado Azure deployment: {} -> {}", modelId, config.getDeploymentName());
            }
        });

        if (!deployments.isEmpty()) {
            providerDeployments.put("azure-openai", deployments);
            log.info("Azure OpenAI registrado com {} modelos: {}", deployments.size(),
                    deployments.stream().map(AIDeployment::getModelId).toList());
        }
    }

    /**
     * Retorna lista de todos os providers disponiveis.
     */
    public Set<String> getAvailableProviders() {
        return providerDeployments.keySet();
    }

    /**
     * Retorna modelos disponiveis para um provider.
     */
    public List<AIDeployment> getModelsForProvider(String provider) {
        return providerDeployments.getOrDefault(provider, Collections.emptyList());
    }

    /**
     * Verifica se um provider esta disponivel.
     */
    public boolean isProviderAvailable(String provider) {
        return providerDeployments.containsKey(provider) && !providerDeployments.get(provider).isEmpty();
    }

    /**
     * Verifica se um modelo esta disponivel em um provider.
     */
    public boolean isModelAvailable(String provider, String modelId) {
        List<AIDeployment> models = providerDeployments.get(provider);
        if (models == null) {
            return false;
        }
        return models.stream().anyMatch(m -> m.getModelId().equals(modelId) && m.isEnabled());
    }

    /**
     * Busca informacoes de um deployment.
     */
    public Optional<AIDeployment> getDeployment(String provider, String modelId) {
        List<AIDeployment> models = providerDeployments.get(provider);
        if (models == null) {
            return Optional.empty();
        }
        return models.stream()
                .filter(m -> m.getModelId().equals(modelId))
                .findFirst();
    }

    /**
     * Retorna total de modelos disponiveis.
     */
    public int getTotalModelsCount() {
        return providerDeployments.values().stream()
                .mapToInt(List::size)
                .sum();
    }
}
