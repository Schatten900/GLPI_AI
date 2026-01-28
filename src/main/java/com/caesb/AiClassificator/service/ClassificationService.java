package com.caesb.AiClassificator.service;

import com.caesb.AiClassificator.client.AIProviderClient;
import com.caesb.AiClassificator.client.AIProviderClient.AIRequest;
import com.caesb.AiClassificator.client.AIProviderClient.AIResponse;
import com.caesb.AiClassificator.client.OpenAIClient;
import com.caesb.AiClassificator.model.ClassificationRequest;
import com.caesb.AiClassificator.model.ClassificationResponse;
import com.caesb.AiClassificator.model.ServiceCatalog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Servico principal de classificacao de tickets com IA.
 * Orquestra o pipeline: Sanitize -> Sentiment -> Prompt -> AI -> Validate -> Response
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassificationService {

    private final Sanitizer sanitizer;
    private final SentimentAnalyzer sentimentAnalyzer;
    private final PromptBuilder promptBuilder;
    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.classification.confidence-threshold:0.75}")
    private double confidenceThreshold;

    @Value("${ai.classification.fallback-queue:Service Desk (1º Nivel)}")
    private String fallbackQueue;

    /**
     * Classifica um ticket usando IA.
     *
     * @param request Dados do ticket para classificacao
     * @return Resultado da classificacao
     */
    public ClassificationResponse classify(ClassificationRequest request) {
        long startTime = System.currentTimeMillis();
        String correlationId = request.getCorrelationId() != null
                ? request.getCorrelationId()
                : UUID.randomUUID().toString();

        log.info("[{}] Iniciando classificacao - subject: {}", correlationId,
                request.getSubject() != null ? request.getSubject().substring(0, Math.min(50, request.getSubject().length())) : "null");

        try {
            // 1. Sanitiza os dados
            Sanitizer.SanitizedData sanitized = sanitizer.sanitizeAll(
                    request.getSubject(),
                    request.getBody(),
                    request.getSenderEmail()
            );

            log.debug("[{}] Dados sanitizados - subject: {}, body length: {}",
                    correlationId, sanitized.getSubject(), sanitized.getBody().length());

            // 2. Analisa sentimento
            SentimentAnalyzer.SentimentResult sentiment = sentimentAnalyzer.analyzeSentiment(
                    sanitized.getBody()
            );

            log.debug("[{}] Sentimento: {}, urgencia: {}, criticidade: {}",
                    correlationId, sentiment.getSentimentLabel(),
                    sentiment.isUrgencyDetected(), sentiment.getCriticalityScore());

            // 3. Constroi o prompt
            PromptBuilder.PromptResult prompt = promptBuilder.buildClassificationPrompt(
                    sanitized.getSubject(),
                    sanitized.getBody(),
                    sentiment.getSentimentLabel(),
                    sentiment.isUrgencyDetected(),
                    null  // RAG context - pode ser adicionado futuramente
            );

            // 4. Envia para a IA
            AIRequest aiRequest = AIRequest.builder()
                    .systemPrompt(prompt.getSystemPrompt())
                    .userPrompt(prompt.getUserPrompt())
                    .model(request.getModel())
                    .build();

            AIResponse aiResponse = openAIClient.sendChatCompletion(aiRequest);

            if (!aiResponse.isSuccess()) {
                log.error("[{}] Erro na classificacao IA: {} - {}",
                        correlationId, aiResponse.getErrorCode(), aiResponse.getErrorMessage());

                return buildErrorResponse(correlationId, aiResponse, startTime, sanitized, sentiment);
            }

            // 5. Parse e valida a resposta
            ClassificationResponse response = parseAndValidateResponse(
                    correlationId, aiResponse, sanitized, sentiment, startTime
            );

            log.info("[{}] Classificacao concluida - tipo: {}, servico: {}, confianca: {}, status: {}",
                    correlationId, response.getType(), response.getServiceId(),
                    response.getConfidenceScore(), response.getStatus());

            return response;

        } catch (Exception e) {
            log.error("[{}] Erro inesperado na classificacao: {}", correlationId, e.getMessage(), e);
            return ClassificationResponse.builder()
                    .success(false)
                    .status("not_applied")
                    .correlationId(correlationId)
                    .errorCode("INTERNAL_ERROR")
                    .errorMessage(e.getMessage())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * Faz parse e valida a resposta da IA.
     */
    private ClassificationResponse parseAndValidateResponse(
            String correlationId,
            AIResponse aiResponse,
            Sanitizer.SanitizedData sanitized,
            SentimentAnalyzer.SentimentResult sentiment,
            long startTime) {

        try {
            JsonNode jsonResponse = objectMapper.readTree(aiResponse.getContent());

            String tipo = jsonResponse.path("tipo").asText("");
            String servicoId = jsonResponse.path("servico_id").asText("");
            String servicoNome = jsonResponse.path("servico_nome").asText("");
            double confidenceScore = jsonResponse.path("confidence_score").asDouble(0.0);

            // Valida o servico
            boolean validService = promptBuilder.isValidServiceId(servicoId);
            String queue = validService
                    ? promptBuilder.getQueueForService(servicoId)
                    : fallbackQueue;

            // Se servico invalido, busca pelo nome no catalogo
            if (!validService && servicoNome != null && !servicoNome.isBlank()) {
                log.warn("[{}] Servico ID '{}' invalido, tentando buscar pelo nome", correlationId, servicoId);
                // Poderia implementar busca por nome aqui
            }

            // Determina status baseado no threshold
            boolean thresholdMet = confidenceScore >= confidenceThreshold && validService;
            String status;
            if (thresholdMet) {
                status = "applied";
            } else if (validService) {
                status = "partial";
            } else {
                status = "manual";
                queue = fallbackQueue;
            }

            // Busca informacoes do servico no catalogo
            ServiceCatalog.Service service = ServiceCatalog.getService(servicoId);
            if (service != null) {
                servicoNome = service.getName();
            }

            return ClassificationResponse.builder()
                    .success(true)
                    .status(status)
                    .correlationId(correlationId)
                    .type(tipo)
                    .serviceId(servicoId)
                    .serviceName(servicoNome)
                    .queue(queue)
                    .confidenceScore(confidenceScore)
                    .thresholdMet(thresholdMet)
                    .sentimentScore(sentiment.getSentimentScore())
                    .sentimentLabel(sentiment.getSentimentLabel())
                    .urgencyDetected(sentiment.isUrgencyDetected())
                    .criticalityScore(sentiment.getCriticalityScore())
                    .shouldIncreaseSeverity(sentiment.isShouldIncreaseSeverity())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .message(thresholdMet
                            ? "Classificacao aplicada automaticamente"
                            : "Classificacao requer revisao manual")
                    .provider(openAIClient.getProviderName())
                    .model(aiResponse.getModel())
                    .sanitizedSubject(sanitized.getSubject())
                    .sanitizedBodySummary(sanitized.getBody())
                    .maskedSender(sanitized.getMaskedSender())
                    .build();

        } catch (Exception e) {
            log.error("[{}] Erro ao fazer parse da resposta da IA: {}", correlationId, e.getMessage());

            return ClassificationResponse.builder()
                    .success(false)
                    .status("not_applied")
                    .correlationId(correlationId)
                    .errorCode("PARSE_ERROR")
                    .errorMessage("Erro ao processar resposta da IA: " + e.getMessage())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .queue(fallbackQueue)
                    .sentimentScore(sentiment.getSentimentScore())
                    .sentimentLabel(sentiment.getSentimentLabel())
                    .urgencyDetected(sentiment.isUrgencyDetected())
                    .sanitizedSubject(sanitized.getSubject())
                    .sanitizedBodySummary(sanitized.getBody())
                    .maskedSender(sanitized.getMaskedSender())
                    .build();
        }
    }

    /**
     * Constroi resposta de erro.
     */
    private ClassificationResponse buildErrorResponse(
            String correlationId,
            AIResponse aiResponse,
            long startTime,
            Sanitizer.SanitizedData sanitized,
            SentimentAnalyzer.SentimentResult sentiment) {

        return ClassificationResponse.builder()
                .success(false)
                .status("not_applied")
                .correlationId(correlationId)
                .errorCode(aiResponse.getErrorCode())
                .errorMessage(aiResponse.getErrorMessage())
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .queue(fallbackQueue)
                .provider(openAIClient.getProviderName())
                .sentimentScore(sentiment.getSentimentScore())
                .sentimentLabel(sentiment.getSentimentLabel())
                .urgencyDetected(sentiment.isUrgencyDetected())
                .sanitizedSubject(sanitized.getSubject())
                .sanitizedBodySummary(sanitized.getBody())
                .maskedSender(sanitized.getMaskedSender())
                .build();
    }
}
