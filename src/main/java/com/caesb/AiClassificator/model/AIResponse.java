package com.caesb.AiClassificator.model;

import lombok.*;

/**
 * Resposta da IA.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIResponse {
    protected boolean success;
    protected String content;
    protected String model;
    protected Integer promptTokens;
    protected Integer completionTokens;
    protected Integer totalTokens;
    protected Long latencyMs;
    protected String errorCode;
    protected String errorMessage;
}