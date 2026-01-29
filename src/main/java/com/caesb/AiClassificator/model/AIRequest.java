package com.caesb.AiClassificator.model;

import lombok.*;

/**
 * Requisicao para a IA.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRequest {
    protected String systemPrompt;
    protected String userPrompt;
    protected String provider;
    protected String model;
    protected Double temperature;
    protected Integer maxTokens;
}