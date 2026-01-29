package com.caesb.AiClassificator.client;

import com.caesb.AiClassificator.config.AzureOpenAIConfig;
import com.caesb.AiClassificator.model.AIRequest;
import com.caesb.AiClassificator.model.AIResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Cliente para Azure OpenAI API com suporte a multiplos deployments.
 * Compativel com GPT-4o, Claude, Gemini e outros modelos disponiveis no Azure.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AzureOpenAIClient implements AIProviderClient {

    private static final String PROVIDER_NAME = "azure-openai";

    private final AzureOpenAIConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @CircuitBreaker(name = "azureopenai", fallbackMethod = "fallbackResponse")
    @Retry(name = "azureopenai")
    public AIResponse sendChatCompletion(AIRequest request) {
        long startTime = System.currentTimeMillis();

        if (!isAvailable()) {
            return AIResponse.builder()
                    .success(false)
                    .errorCode("NOT_CONFIGURED")
                    .errorMessage("Azure OpenAI nao esta configurado ou habilitado")
                    .build();
        }

        String modelId = request.getModel();
        if (modelId == null || modelId.isBlank()) {
            return AIResponse.builder()
                    .success(false)
                    .errorCode("NO_MODEL")
                    .errorMessage("Model ID nao especificado para Azure OpenAI")
                    .build();
        }

        // Busca configuracao do deployment
        AzureOpenAIConfig.DeploymentConfig deployment = config.getDeployments().get(modelId);
        if (deployment == null || !deployment.isEnabled()) {
            return AIResponse.builder()
                    .success(false)
                    .errorCode("INVALID_MODEL")
                    .errorMessage("Modelo '" + modelId + "' nao encontrado ou desabilitado")
                    .build();
        }

        try {
            // Constroi URL do endpoint
            String endpoint = config.buildEndpointUrl(deployment.getDeploymentName());

            // Prepara headers (Azure usa api-key ao inves de Bearer token)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", config.getApiKey());

            // Determina parametros (request > deployment > config default)
            Double temperature = request.getTemperature() != null
                    ? request.getTemperature()
                    : (deployment.getTemperature() != null ? deployment.getTemperature() : config.getDefaultTemperature());
            Integer maxTokens = request.getMaxTokens() != null
                    ? request.getMaxTokens()
                    : (deployment.getMaxTokens() != null ? deployment.getMaxTokens() : config.getDefaultMaxTokens());

            // Prepara body da requisicao
            Map<String, Object> body = Map.of(
                    "messages", List.of(
                            Map.of("role", "system", "content", request.getSystemPrompt()),
                            Map.of("role", "user", "content", request.getUserPrompt())
                    ),
                    "temperature", temperature,
                    "max_tokens", maxTokens,
                    "response_format", Map.of("type", "json_object")
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            log.debug("Enviando requisicao para Azure OpenAI - deployment: {}, model: {}",
                    deployment.getDeploymentName(), modelId);

            // Faz a requisicao
            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            long latencyMs = System.currentTimeMillis() - startTime;

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseResponse(response.getBody(), modelId, latencyMs);
            } else {
                return AIResponse.builder()
                        .success(false)
                        .errorCode("HTTP_" + response.getStatusCode().value())
                        .errorMessage("Resposta invalida do Azure OpenAI")
                        .latencyMs(latencyMs)
                        .model(modelId)
                        .build();
            }

        } catch (RestClientException e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.error("Erro ao chamar Azure OpenAI API: {}", e.getMessage());
            return AIResponse.builder()
                    .success(false)
                    .errorCode("REST_ERROR")
                    .errorMessage(e.getMessage())
                    .latencyMs(latencyMs)
                    .model(modelId)
                    .build();
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.error("Erro inesperado ao chamar Azure OpenAI API: {}", e.getMessage(), e);
            return AIResponse.builder()
                    .success(false)
                    .errorCode("UNKNOWN_ERROR")
                    .errorMessage(e.getMessage())
                    .latencyMs(latencyMs)
                    .model(modelId)
                    .build();
        }
    }

    /**
     * Fallback em caso de circuit breaker aberto.
     */
    public AIResponse fallbackResponse(AIRequest request, Throwable t) {
        log.warn("Circuit breaker ativado para Azure OpenAI. Motivo: {}", t.getMessage());
        return AIResponse.builder()
                .success(false)
                .errorCode("CIRCUIT_BREAKER")
                .errorMessage("Azure OpenAI temporariamente indisponivel: " + t.getMessage())
                .model(request.getModel())
                .build();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isAvailable() {
        return config.isConfigured();
    }

    /**
     * Verifica se um modelo especifico esta disponivel.
     */
    public boolean isModelAvailable(String modelId) {
        if (!isAvailable() || modelId == null) {
            return false;
        }
        AzureOpenAIConfig.DeploymentConfig deployment = config.getDeployments().get(modelId);
        return deployment != null && deployment.isEnabled();
    }

    /**
     * Testa conectividade com um modelo especifico.
     */
    public AIResponse testConnection(String modelId) {
        AIRequest testRequest = AIRequest.builder()
                .systemPrompt("Responda apenas com JSON: {\"status\": \"ok\"}")
                .userPrompt("Teste de conexao")
                .model(modelId)
                .maxTokens(20)
                .temperature(0.0)
                .build();

        return sendChatCompletion(testRequest);
    }

    /**
     * Faz parse da resposta do Azure OpenAI.
     */
    private AIResponse parseResponse(String responseBody, String model, long latencyMs) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // Extrai conteudo da resposta
            String content = root.path("choices").get(0)
                    .path("message").path("content").asText();

            // Extrai tokens de uso
            JsonNode usage = root.path("usage");
            Integer promptTokens = usage.path("prompt_tokens").asInt(0);
            Integer completionTokens = usage.path("completion_tokens").asInt(0);
            Integer totalTokens = usage.path("total_tokens").asInt(0);

            log.debug("Resposta Azure OpenAI - model: {}, tokens: prompt={}, completion={}, total={}, latency={}ms",
                    model, promptTokens, completionTokens, totalTokens, latencyMs);

            return AIResponse.builder()
                    .success(true)
                    .content(content)
                    .model(model)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(totalTokens)
                    .latencyMs(latencyMs)
                    .build();

        } catch (Exception e) {
            log.error("Erro ao fazer parse da resposta Azure OpenAI: {}", e.getMessage());
            return AIResponse.builder()
                    .success(false)
                    .errorCode("PARSE_ERROR")
                    .errorMessage("Erro ao processar resposta do Azure OpenAI: " + e.getMessage())
                    .latencyMs(latencyMs)
                    .model(model)
                    .build();
        }
    }
}
