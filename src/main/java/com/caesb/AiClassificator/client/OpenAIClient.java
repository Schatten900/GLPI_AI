package com.caesb.AiClassificator.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Cliente para a API da OpenAI com suporte a circuit breaker e retry.
 * Compativel com OpenAI e APIs compativeis (Azure OpenAI, LocalAI, etc).
 */
@Slf4j
@Component
public class OpenAIClient implements AIProviderClient {

    private static final String PROVIDER_NAME = "OpenAI";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    private static final String DEFAULT_ENDPOINT = "https://api.openai.com/v1/chat/completions";

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    @Value("${ai.openai.endpoint:" + DEFAULT_ENDPOINT + "}")
    private String endpoint;

    @Value("${ai.openai.model:" + DEFAULT_MODEL + "}")
    private String defaultModel;

    @Value("${ai.openai.temperature:0.3}")
    private Double defaultTemperature;

    @Value("${ai.openai.max-tokens:500}")
    private Integer defaultMaxTokens;

    @Value("${ai.openai.timeout:30000}")
    private Integer timeout;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAIClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @CircuitBreaker(name = "openai", fallbackMethod = "fallbackResponse")
    @Retry(name = "openai")
    public AIResponse sendChatCompletion(AIRequest request) {
        long startTime = System.currentTimeMillis();

        if (!isAvailable()) {
            return AIResponse.builder()
                    .success(false)
                    .errorCode("NO_API_KEY")
                    .errorMessage("API key nao configurada para OpenAI")
                    .build();
        }

        try {
            // Prepara headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // Prepara body da requisicao
            String model = request.getModel() != null ? request.getModel() : defaultModel;
            Double temperature = request.getTemperature() != null ? request.getTemperature() : defaultTemperature;
            Integer maxTokens = request.getMaxTokens() != null ? request.getMaxTokens() : defaultMaxTokens;

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", request.getSystemPrompt()),
                            Map.of("role", "user", "content", request.getUserPrompt())
                    ),
                    "temperature", temperature,
                    "max_tokens", maxTokens,
                    "response_format", Map.of("type", "json_object")
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            log.debug("Enviando requisicao para OpenAI - model: {}", model);

            // Faz a requisicao
            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            long latencyMs = System.currentTimeMillis() - startTime;

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseResponse(response.getBody(), model, latencyMs);
            } else {
                return AIResponse.builder()
                        .success(false)
                        .errorCode("HTTP_" + response.getStatusCode().value())
                        .errorMessage("Resposta invalida da OpenAI")
                        .latencyMs(latencyMs)
                        .build();
            }

        } catch (RestClientException e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.error("Erro ao chamar OpenAI API: {}", e.getMessage());
            return AIResponse.builder()
                    .success(false)
                    .errorCode("REST_ERROR")
                    .errorMessage(e.getMessage())
                    .latencyMs(latencyMs)
                    .build();
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.error("Erro inesperado ao chamar OpenAI API: {}", e.getMessage(), e);
            return AIResponse.builder()
                    .success(false)
                    .errorCode("UNKNOWN_ERROR")
                    .errorMessage(e.getMessage())
                    .latencyMs(latencyMs)
                    .build();
        }
    }

    /**
     * Fallback em caso de circuit breaker aberto.
     */
    public AIResponse fallbackResponse(AIRequest request, Throwable t) {
        log.warn("Circuit breaker ativado para OpenAI. Motivo: {}", t.getMessage());
        return AIResponse.builder()
                .success(false)
                .errorCode("CIRCUIT_BREAKER")
                .errorMessage("Servico OpenAI temporariamente indisponivel: " + t.getMessage())
                .build();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Faz parse da resposta da OpenAI.
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

            log.debug("Resposta OpenAI - tokens: prompt={}, completion={}, total={}, latency={}ms",
                    promptTokens, completionTokens, totalTokens, latencyMs);

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
            log.error("Erro ao fazer parse da resposta OpenAI: {}", e.getMessage());
            return AIResponse.builder()
                    .success(false)
                    .errorCode("PARSE_ERROR")
                    .errorMessage("Erro ao processar resposta da OpenAI: " + e.getMessage())
                    .latencyMs(latencyMs)
                    .build();
        }
    }
}
