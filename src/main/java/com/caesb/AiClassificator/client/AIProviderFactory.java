package com.caesb.AiClassificator.client;

import com.caesb.AiClassificator.model.AIRequest;
import com.caesb.AiClassificator.model.AIResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Factory para selecionar o cliente de IA apropriado com base no provider/model.
 * Centraliza a logica de roteamento para diferentes providers.
 * Implementa fallback automatico para modelo secundario em caso de falha.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AIProviderFactory {

    private static final String FALLBACK_MODEL = "gpt-4o-mini";

    private final AIProviderRegistry registry;
    private final AzureOpenAIClient azureOpenAIClient;

    /**
     * Envia requisicao para o provider/model especificado.
     * Se provider/model nao especificado, usa defaults do registry.
     * Implementa fallback automatico em caso de falha.
     *
     * @param request Requisicao com provider e model (opcionais)
     * @return Resposta da IA
     */
    public AIResponse sendRequest(AIRequest request) {
        String provider = resolveProvider(request.getProvider());
        String model = resolveModel(provider, request.getModel());

        log.debug("Roteando requisicao para provider: {}, model: {}", provider, model);

        // Valida se provider esta disponivel
        if (!registry.isProviderAvailable(provider)) {
            log.error("Provider {} nao disponivel, encaminhando para classificacao manual", provider);
            return buildManualFallbackResponse("Provider '" + provider + "' nao disponivel");
        }

        // Valida se model esta disponivel
        if (!registry.isModelAvailable(provider, model)) {
            log.warn("Modelo {} nao disponivel, tentando fallback para {}", model, FALLBACK_MODEL);
            // Tenta usar o fallback diretamente
            if (registry.isModelAvailable(provider, FALLBACK_MODEL)) {
                model = FALLBACK_MODEL;
            } else {
                return buildManualFallbackResponse("Modelo '" + model + "' nao disponivel");
            }
        }

        // Cria request com provider/model resolvidos
        AIRequest resolvedRequest = AIRequest.builder()
                .systemPrompt(request.getSystemPrompt())
                .userPrompt(request.getUserPrompt())
                .model(model)
                .temperature(request.getTemperature())
                .maxTokens(request.getMaxTokens())
                .provider(provider)
                .build();

        // Tenta modelo principal
        AIResponse response = routeToClient(provider, resolvedRequest);

        if (response.isSuccess()) {
            return response;
        }

        // Fallback: tenta modelo secundario se nao for o mesmo
        if (!FALLBACK_MODEL.equals(model) && registry.isModelAvailable(provider, FALLBACK_MODEL)) {
            log.warn("Modelo {} falhou ({}), tentando fallback para {}",
                    model, response.getErrorCode(), FALLBACK_MODEL);

            AIRequest fallbackRequest = AIRequest.builder()
                    .systemPrompt(request.getSystemPrompt())
                    .userPrompt(request.getUserPrompt())
                    .model(FALLBACK_MODEL)
                    .temperature(request.getTemperature())
                    .maxTokens(request.getMaxTokens())
                    .provider(provider)
                    .build();

            AIResponse fallbackResponse = routeToClient(provider, fallbackRequest);

            if (fallbackResponse.isSuccess()) {
                log.info("Fallback para {} bem sucedido", FALLBACK_MODEL);
                return fallbackResponse;
            }

            log.error("Fallback {} tambem falhou: {}", FALLBACK_MODEL, fallbackResponse.getErrorCode());
        }

        // Todos os modelos falharam - encaminha para classificacao manual
        log.error("Todos os modelos falharam - encaminhando para classificacao manual");
        return buildManualFallbackResponse("IA temporariamente indisponivel");
    }

    /**
     * Roteia requisicao para o client apropriado.
     */
    private AIResponse routeToClient(String provider, AIRequest request) {
        return switch (provider) {
            case "azure-openai" -> azureOpenAIClient.sendChatCompletion(request);
            default -> AIResponse.builder()
                    .success(false)
                    .errorCode("UNKNOWN_PROVIDER")
                    .errorMessage("Provider desconhecido: " + provider)
                    .build();
        };
    }

    /**
     * Constroi resposta indicando que classificacao manual e necessaria.
     * O ClassificationService interpreta isso e envia para fallback_queue.
     */
    private AIResponse buildManualFallbackResponse(String reason) {
        return AIResponse.builder()
                .success(false)
                .errorCode("AI_UNAVAILABLE")
                .errorMessage(reason)
                .build();
    }

    /**
     * Testa conectividade com um provider/model especifico.
     */
    public AIResponse testConnection(String provider, String model) {
        String resolvedProvider = resolveProvider(provider);
        String resolvedModel = resolveModel(resolvedProvider, model);

        log.info("Testando conexao com {}/{}", resolvedProvider, resolvedModel);

        if (!registry.isProviderAvailable(resolvedProvider)) {
            return AIResponse.builder()
                    .success(false)
                    .errorCode("PROVIDER_UNAVAILABLE")
                    .errorMessage("Provider '" + resolvedProvider + "' nao esta disponivel")
                    .build();
        }

        return switch (resolvedProvider) {
            case "azure-openai" -> azureOpenAIClient.testConnection(resolvedModel);

            default -> AIResponse.builder()
                    .success(false)
                    .errorCode("UNKNOWN_PROVIDER")
                    .errorMessage("Provider desconhecido: " + resolvedProvider)
                    .build();
        };
    }

    /**
     * Retorna o client para um provider especifico.
     */
    public AIProviderClient getClient(String provider) {
        return switch (provider) {
            case "azure-openai" -> azureOpenAIClient;
            default -> null;
        };
    }

    /**
     * Resolve provider - usa default se nao especificado.
     */
    private String resolveProvider(String provider) {
        if (provider != null && !provider.isBlank()) {
            return provider.toLowerCase().trim();
        }
        return registry.getDefaultProvider();
    }

    /**
     * Resolve model - usa default do provider se nao especificado.
     */
    private String resolveModel(String provider, String model) {
        if (model != null && !model.isBlank()) {
            return model.trim();
        }

        // Se provider e o default, usa o model default
        if (provider.equals(registry.getDefaultProvider())) {
            return registry.getDefaultModel();
        }

        // Caso contrario, usa o primeiro modelo disponivel do provider
        var models = registry.getModelsForProvider(provider);
        if (!models.isEmpty()) {
            return models.get(0).getModelId();
        }

        return registry.getDefaultModel();
    }
}
