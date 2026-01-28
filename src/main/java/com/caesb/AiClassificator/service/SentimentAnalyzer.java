package com.caesb.AiClassificator.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analisador de sentimento baseado em lexico para texto em portugues.
 * Detecta sentimento (positivo/negativo/neutro) e urgencia/criticidade.
 */
@Slf4j
@Service
public class SentimentAnalyzer {

    // Lexico de palavras positivas com pesos
    private static final Map<String, Double> POSITIVE_LEXICON = new HashMap<>();

    // Lexico de palavras negativas com pesos
    private static final Map<String, Double> NEGATIVE_LEXICON = new HashMap<>();

    // Lexico de palavras de urgencia
    private static final Map<String, Double> URGENCY_LEXICON = new HashMap<>();

    static {
        // Palavras positivas
        POSITIVE_LEXICON.put("bom", 1.0);
        POSITIVE_LEXICON.put("otimo", 2.0);
        POSITIVE_LEXICON.put("ótimo", 2.0);
        POSITIVE_LEXICON.put("excelente", 2.0);
        POSITIVE_LEXICON.put("maravilhoso", 2.0);
        POSITIVE_LEXICON.put("feliz", 1.0);
        POSITIVE_LEXICON.put("satisfeito", 1.0);
        POSITIVE_LEXICON.put("agradecido", 1.0);
        POSITIVE_LEXICON.put("obrigado", 1.0);
        POSITIVE_LEXICON.put("gostei", 1.0);
        POSITIVE_LEXICON.put("amo", 2.0);
        POSITIVE_LEXICON.put("perfeito", 2.0);
        POSITIVE_LEXICON.put("funcionando", 1.0);
        POSITIVE_LEXICON.put("resolvido", 1.0);
        POSITIVE_LEXICON.put("ajuda", 1.0);
        POSITIVE_LEXICON.put("suporte", 1.0);
        POSITIVE_LEXICON.put("rapido", 1.0);
        POSITIVE_LEXICON.put("rápido", 1.0);
        POSITIVE_LEXICON.put("eficiente", 1.5);

        // Palavras negativas
        NEGATIVE_LEXICON.put("ruim", 1.0);
        NEGATIVE_LEXICON.put("pessimo", 2.0);
        NEGATIVE_LEXICON.put("péssimo", 2.0);
        NEGATIVE_LEXICON.put("terrivel", 2.0);
        NEGATIVE_LEXICON.put("terrível", 2.0);
        NEGATIVE_LEXICON.put("horrivel", 2.0);
        NEGATIVE_LEXICON.put("horrível", 2.0);
        NEGATIVE_LEXICON.put("triste", 1.0);
        NEGATIVE_LEXICON.put("insatisfeito", 1.0);
        NEGATIVE_LEXICON.put("frustrado", 1.0);
        NEGATIVE_LEXICON.put("problema", 1.0);
        NEGATIVE_LEXICON.put("erro", 1.0);
        NEGATIVE_LEXICON.put("falha", 1.0);
        NEGATIVE_LEXICON.put("quebrado", 1.0);
        NEGATIVE_LEXICON.put("nao funciona", 2.0);
        NEGATIVE_LEXICON.put("não funciona", 2.0);
        NEGATIVE_LEXICON.put("odeio", 2.0);
        NEGATIVE_LEXICON.put("detesto", 2.0);
        NEGATIVE_LEXICON.put("reclamacao", 1.0);
        NEGATIVE_LEXICON.put("reclamação", 1.0);
        NEGATIVE_LEXICON.put("lento", 1.0);
        NEGATIVE_LEXICON.put("travando", 1.5);
        NEGATIVE_LEXICON.put("demora", 1.0);
        NEGATIVE_LEXICON.put("impossivel", 1.5);
        NEGATIVE_LEXICON.put("impossível", 1.5);

        // Palavras de urgencia/criticidade
        URGENCY_LEXICON.put("urgente", 1.0);
        URGENCY_LEXICON.put("emergencia", 1.0);
        URGENCY_LEXICON.put("emergência", 1.0);
        URGENCY_LEXICON.put("critico", 1.0);
        URGENCY_LEXICON.put("crítico", 1.0);
        URGENCY_LEXICON.put("critica", 1.0);
        URGENCY_LEXICON.put("crítica", 1.0);
        URGENCY_LEXICON.put("imediato", 1.0);
        URGENCY_LEXICON.put("imediatamente", 1.0);
        URGENCY_LEXICON.put("asap", 1.0);
        URGENCY_LEXICON.put("agora", 1.0);
        URGENCY_LEXICON.put("prioritario", 1.0);
        URGENCY_LEXICON.put("prioritário", 1.0);
        URGENCY_LEXICON.put("prioridade", 1.0);
        URGENCY_LEXICON.put("grave", 1.0);
        URGENCY_LEXICON.put("serio", 1.0);
        URGENCY_LEXICON.put("sério", 1.0);
        URGENCY_LEXICON.put("parado", 1.0);
        URGENCY_LEXICON.put("travado", 1.0);
        URGENCY_LEXICON.put("bloqueado", 1.0);
        URGENCY_LEXICON.put("indisponivel", 1.0);
        URGENCY_LEXICON.put("indisponível", 1.0);
        URGENCY_LEXICON.put("caiu", 1.0);
        URGENCY_LEXICON.put("quebrou", 1.0);
        URGENCY_LEXICON.put("offline", 1.0);
        URGENCY_LEXICON.put("fora do ar", 1.0);
    }

    /**
     * Analisa o sentimento e urgencia no texto.
     *
     * @param body Texto a ser analisado
     * @return Resultado da analise com scores e labels
     */
    public SentimentResult analyzeSentiment(String body) {
        if (body == null || body.isBlank()) {
            return SentimentResult.builder()
                    .sentimentScore(0.0)
                    .sentimentLabel("neutral")
                    .urgencyDetected(false)
                    .criticalityScore(0)
                    .shouldIncreaseSeverity(false)
                    .build();
        }

        String textLower = body.toLowerCase(Locale.ROOT);

        // Calcula scores positivo e negativo
        double positiveScore = calculateScore(textLower, POSITIVE_LEXICON);
        double negativeScore = calculateScore(textLower, NEGATIVE_LEXICON);

        // Score de sentimento (-1 a 1)
        double sentimentScore = positiveScore - negativeScore;
        // Normaliza para intervalo -1 a 1
        sentimentScore = Math.max(-1.0, Math.min(1.0, sentimentScore));

        // Label do sentimento
        String sentimentLabel;
        if (sentimentScore > 0.1) {
            sentimentLabel = "positive";
        } else if (sentimentScore < -0.1) {
            sentimentLabel = "negative";
        } else {
            sentimentLabel = "neutral";
        }

        // Detecta urgencia
        boolean urgencyDetected = detectUrgency(textLower);

        // Calcula score de criticidade (0-3)
        int criticalityScore = 0;
        if ("negative".equals(sentimentLabel)) {
            criticalityScore += 1;
        }
        if (urgencyDetected) {
            criticalityScore += 2;
        }

        // Deve aumentar severidade se criticidade >= 2
        boolean shouldIncreaseSeverity = criticalityScore >= 2;

        return SentimentResult.builder()
                .sentimentScore(Math.round(sentimentScore * 100.0) / 100.0)  // 2 casas decimais
                .sentimentLabel(sentimentLabel)
                .urgencyDetected(urgencyDetected)
                .criticalityScore(criticalityScore)
                .shouldIncreaseSeverity(shouldIncreaseSeverity)
                .build();
    }

    /**
     * Calcula o score baseado no lexico.
     */
    private double calculateScore(String text, Map<String, Double> lexicon) {
        double score = 0.0;

        for (Map.Entry<String, Double> entry : lexicon.entrySet()) {
            String word = entry.getKey();
            Double weight = entry.getValue();

            // Conta ocorrencias da palavra
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);
            int count = 0;
            while (matcher.find()) {
                count++;
            }

            score += count * weight;
        }

        return score;
    }

    /**
     * Detecta se ha indicadores de urgencia no texto.
     */
    private boolean detectUrgency(String text) {
        for (String word : URGENCY_LEXICON.keySet()) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resultado da analise de sentimento.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SentimentResult {
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
}
