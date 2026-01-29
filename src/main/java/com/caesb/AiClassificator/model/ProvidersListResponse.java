package com.caesb.AiClassificator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response com lista de providers e modelos disponiveis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvidersListResponse {

    /**
     * Lista de providers disponiveis.
     */
    private List<ProviderInfo> providers;

    /**
     * Provider padrao.
     */
    private String defaultProvider;

    /**
     * Modelo padrao.
     */
    private String defaultModel;

    /**
     * Total de modelos disponiveis.
     */
    private int totalModels;
}
