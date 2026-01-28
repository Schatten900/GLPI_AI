package com.caesb.AiClassificator.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Interface para clientes de provedores de IA (OpenAI, Claude, Gemini, etc).
 */
public interface AIProviderClient {

    /**
     * Envia uma requisicao de chat completion para a IA.
     *
     * @param request Requisicao com prompts e configuracoes
     * @return Resposta da IA
     */
    AIResponse sendChatCompletion(AIRequest request);

    /**
     * Retorna o nome do provider.
     */
    String getProviderName();

    /**
     * Verifica se o provider esta disponivel.
     */
    boolean isAvailable();

    /**
     * Requisicao para a IA.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class AIRequest {
        private String systemPrompt;
        private String userPrompt;
        private String model;
        private Double temperature;
        private Integer maxTokens;
    }

    /**
     * Resposta da IA.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class AIResponse {
        private boolean success;
        private String content;
        private String model;
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
        private Long latencyMs;
        private String errorCode;
        private String errorMessage;
    }
}
