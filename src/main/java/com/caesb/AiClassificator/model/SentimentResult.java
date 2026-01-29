package com.caesb.AiClassificator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado da analise de sentimento.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentimentResult {
    /**
     * Score de sentimento (-1.0 a 1.0).
     * Valores negativos indicam sentimento negativo.
     */
    private double sentimentScore;

    /**
     * Label do sentimento: positive, neutral, negative.
     */
    private String sentimentLabel;

    /**
     * Indica se foi detectada urgencia no texto.
     */
    private boolean urgencyDetected;

    /**
     * Score de criticidade (0-3).
     * 0: Normal
     * 1: Negativo
     * 2: Urgente
     * 3: Negativo + Urgente
     */
    private int criticalityScore;

    /**
     * Indica se a severidade deve ser aumentada.
     * True se criticalityScore >= 2.
     */
    private boolean shouldIncreaseSeverity;
}