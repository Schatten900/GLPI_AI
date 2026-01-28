package com.caesb.AiClassificator.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response model para classificacao de tickets com IA.
 * Retorna tipo, servico, fila e score de confianca.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClassificationResponse {

    /**
     * Indica se a classificacao foi bem-sucedida.
     */
    private boolean success;

    /**
     * Status da classificacao: applied, partial, manual, not_applied.
     */
    private String status;

    /**
     * ID de correlacao para rastreamento.
     */
    private String correlationId;

    /**
     * Tipo classificado: REQ (Requisicao), INC (Incidente), OS (Ordem de Servico).
     */
    private String type;

    /**
     * ID do servico no catalogo CAESB (ex: REQ-101, INC-102).
     */
    private String serviceId;

    /**
     * Nome do servico (ex: "Resetar Senha de Usuario").
     */
    private String serviceName;

    /**
     * Fila/dominio para onde o ticket deve ser direcionado.
     */
    private String queue;

    /**
     * Score de confianca da classificacao (0.0 a 1.0).
     */
    private Double confidenceScore;

    /**
     * Indica se a confianca atendeu o threshold para aplicacao automatica.
     */
    private boolean thresholdMet;

    /**
     * Score de sentimento (-1.0 a 1.0).
     */
    private Double sentimentScore;

    /**
     * Label do sentimento: positive, neutral, negative.
     */
    private String sentimentLabel;

    /**
     * Indica se urgencia foi detectada no texto.
     */
    private boolean urgencyDetected;

    /**
     * Score de criticidade (0-3).
     */
    private Integer criticalityScore;

    /**
     * Indica se a severidade deve ser aumentada.
     */
    private boolean shouldIncreaseSeverity;

    /**
     * Tempo de processamento em milissegundos.
     */
    private Long processingTimeMs;

    /**
     * Mensagem descritiva do resultado.
     */
    private String message;

    /**
     * Codigo de erro, se houver.
     */
    private String errorCode;

    /**
     * Mensagem de erro detalhada, se houver.
     */
    private String errorMessage;

    /**
     * Provider utilizado (OpenAI, Claude, Mock).
     */
    private String provider;

    /**
     * Modelo utilizado.
     */
    private String model;

    /**
     * Subject sanitizado enviado para a IA.
     */
    private String sanitizedSubject;

    /**
     * Resumo do body sanitizado enviado para a IA.
     */
    private String sanitizedBodySummary;

    /**
     * Email do remetente mascarado.
     */
    private String maskedSender;
}
