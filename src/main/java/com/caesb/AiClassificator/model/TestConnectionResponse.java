package com.caesb.AiClassificator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response do teste de conectividade com um provider/modelo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestConnectionResponse {

    /**
     * Se o teste foi bem sucedido.
     */
    private boolean success;

    /**
     * Provider testado.
     */
    private String provider;

    /**
     * Modelo testado.
     */
    private String model;

    /**
     * Latencia em milissegundos.
     */
    private Long latencyMs;

    /**
     * Mensagem descritiva.
     */
    private String message;

    /**
     * Codigo de erro (se falhou).
     */
    private String errorCode;

    /**
     * Mensagem de erro (se falhou).
     */
    private String errorMessage;
}
