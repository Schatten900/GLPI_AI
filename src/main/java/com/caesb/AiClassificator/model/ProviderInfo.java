package com.caesb.AiClassificator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO com informacoes de um provider de IA.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderInfo {

    /**
     * ID do provider (ex: azure-openai, openai).
     */
    private String id;

    /**
     * Nome de exibicao do provider.
     */
    private String displayName;

    /**
     * Se o provider esta habilitado.
     */
    private boolean enabled;

    /**
     * Lista de modelos disponiveis neste provider.
     */
    private List<ModelInfo> models;

    /**
     * Informacoes de um modelo.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelInfo {
        private String id;
        private String displayName;
        private String description;
        private boolean enabled;
    }
}
