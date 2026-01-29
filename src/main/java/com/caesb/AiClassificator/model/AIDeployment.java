package com.caesb.AiClassificator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa um deployment de modelo de IA configurado.
 * Usado para Azure OpenAI e outros providers que usam conceito de deployment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIDeployment {

    /**
     * ID do modelo (ex: gpt-4o-mini, claude-3-sonnet).
     */
    private String modelId;

    /**
     * Nome do deployment no Azure (ex: gpt-4o-mini-deploy).
     */
    private String deploymentName;

    /**
     * Nome de exibicao para UI.
     */
    private String displayName;

    /**
     * Descricao do modelo.
     */
    private String description;

    /**
     * Se o deployment esta habilitado.
     */
    private boolean enabled;

    /**
     * Temperatura padrao para este modelo.
     */
    private Double defaultTemperature;

    /**
     * Max tokens padrao para este modelo.
     */
    private Integer defaultMaxTokens;
}
