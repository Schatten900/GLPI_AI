package com.caesb.AiClassificator.service;

import com.caesb.AiClassificator.client.AIProviderFactory;
import com.caesb.AiClassificator.client.AIProviderRegistry;
import com.caesb.AiClassificator.model.AIRequest;
import com.caesb.AiClassificator.model.AIResponse;
import com.caesb.AiClassificator.model.ClassificationRequest;
import com.caesb.AiClassificator.model.ClassificationResponse;
import com.caesb.AiClassificator.model.ServiceCatalog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.caesb.AiClassificator.model.*;

import java.util.Optional;
import java.util.UUID;

/**
 * Servico principal de classificacao de tickets com IA.
 * Orquestra o pipeline: Cache -> Sanitize -> Sentiment -> Prompt -> AI -> Validate -> Response
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassificationService {

    private final Sanitizer sanitizer;
    private final SentimentAnalyzer sentimentAnalyzer;
    private final PromptBuilder promptBuilder;
    private final AIProviderFactory aiProviderFactory;
    private final AIProviderRegistry aiProviderRegistry;
    private final ClassificationCache cache;
    private final ObjectMapper objectMapper;

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

        log.info("[{}] Iniciando classificacao - ticketId: {}", correlationId,
                request.getTicketId() != null ? request.getTicketId() : "N/A");

        try {
            // 0. Verifica cache (idempotencia)
            Optional<ClassificationResponse> cached = cache.get(
                    request.getTicketId(), request.getSubject(), request.getBody());
            if (cached.isPresent()) {
                log.info("[{}] Retornando resposta do cache - ticketId: {}",
                        correlationId, request.getTicketId());
                return cached.get();
            }

            // 1. Sanitiza os dados
            SanitizedData sanitized = sanitizer.sanitizeAll(
                    request.getSubject(),
                    request.getBody(),
                    request.getSenderEmail()
            );

            log.debug("[{}] Dados sanitizados - subject length: {}, body length: {}",
                    correlationId, sanitized.getSubject().length(), sanitized.getBody().length());

            // 2. Analisa sentimento
            SentimentResult sentiment = sentimentAnalyzer.analyzeSentiment(
                    sanitized.getBody()
            );

            log.debug("[{}] Sentimento: {}, urgencia: {}, criticidade: {}",
                    correlationId, sentiment.getSentimentLabel(),
                    sentiment.isUrgencyDetected(), sentiment.getCriticalityScore());

            // 3. Constroi o prompt
            PromptResult prompt = promptBuilder.buildClassificationPrompt(
                    sanitized.getSubject(),
                    sanitized.getBody(),
                    sentiment.getSentimentLabel(),
                    sentiment.isUrgencyDetected(),
                    null  // RAG context - pode ser adicionado futuramente
            );

            // 4. Envia para a IA (via factory que roteia para o provider correto)
            String provider = request.getProvider() != null ? request.getProvider() : aiProviderRegistry.getDefaultProvider();
            String model = request.getModel() != null ? request.getModel() : aiProviderRegistry.getDefaultModel();

            com.caesb.AiClassificator.model.AIRequest aiRequest = com.caesb.AiClassificator.model.AIRequest.builder()
                    .systemPrompt(prompt.getSystemPrompt())
                    .userPrompt(prompt.getUserPrompt())
                    .provider(provider)
                    .model(model)
                    .build();

            log.debug("[{}] Usando provider: {}, model: {}", correlationId, provider, model);

            com.caesb.AiClassificator.model.AIResponse aiResponse = aiProviderFactory.sendRequest(aiRequest);

            if (!aiResponse.isSuccess()) {
                log.error("[{}] Erro na classificacao IA: {} - {}",
                        correlationId, aiResponse.getErrorCode(), aiResponse.getErrorMessage());

                // Se IA indisponivel, encaminha para classificacao manual
                if ("AI_UNAVAILABLE".equals(aiResponse.getErrorCode())) {
                    log.warn("[{}] IA indisponivel, encaminhando para classificacao manual", correlationId);
                    ClassificationResponse manualResponse = ClassificationResponse.builder()
                            .success(true)  // Requisicao processada com sucesso
                            .status("manual")
                            .correlationId(correlationId)
                            .queue(fallbackQueue)
                            .message("IA temporariamente indisponivel - classificacao manual necessaria")
                            .sentimentScore(sentiment.getSentimentScore())
                            .sentimentLabel(sentiment.getSentimentLabel())
                            .urgencyDetected(sentiment.isUrgencyDetected())
                            .criticalityScore(sentiment.getCriticalityScore())
                            .shouldIncreaseSeverity(sentiment.isShouldIncreaseSeverity())
                            .processingTimeMs(System.currentTimeMillis() - startTime)
                            .sanitizedSubject(sanitized.getSubject())
                            .sanitizedBodySummary(sanitized.getBody())
                            .maskedSender(sanitized.getMaskedSender())
                            .build();

                    // Armazena no cache
                    cache.put(request.getTicketId(), request.getSubject(), request.getBody(), manualResponse);
                    return manualResponse;
                }

                return buildErrorResponse(correlationId, aiResponse, startTime, sanitized, sentiment);
            }

            // 5. Parse e valida a resposta
            ClassificationResponse response = parseAndValidateResponse(
                    correlationId, aiResponse, sanitized, sentiment, startTime, provider
            );

            log.info("[{}] Classificacao concluida - tipo: {}, servico: {}, confianca: {}, status: {}",
                    correlationId, response.getType(), response.getServiceId(),
                    response.getConfidenceScore(), response.getStatus());

            // 6. Armazena no cache se sucesso
            if (response.isSuccess()) {
                cache.put(request.getTicketId(), request.getSubject(), request.getBody(), response);
            }

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
            com.caesb.AiClassificator.model.AIResponse aiResponse,
            SanitizedData sanitized,
            SentimentResult sentiment,
            long startTime,
            String provider) {

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
            com.caesb.AiClassificator.model.Service service =  ServiceCatalog.getService(servicoId);
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
                    .provider(provider)
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
            com.caesb.AiClassificator.model.AIResponse aiResponse,
            long startTime,
            SanitizedData sanitized,
            SentimentResult sentiment) {

        return ClassificationResponse.builder()
                .success(false)
                .status("not_applied")
                .correlationId(correlationId)
                .errorCode(aiResponse.getErrorCode())
                .errorMessage(aiResponse.getErrorMessage())
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .queue(fallbackQueue)
                .provider(aiProviderRegistry.getDefaultProvider())
                .model(aiResponse.getModel())
                .sentimentScore(sentiment.getSentimentScore())
                .sentimentLabel(sentiment.getSentimentLabel())
                .urgencyDetected(sentiment.isUrgencyDetected())
                .sanitizedSubject(sanitized.getSubject())
                .sanitizedBodySummary(sanitized.getBody())
                .maskedSender(sanitized.getMaskedSender())
                .build();
    }
}
