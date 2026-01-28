package com.caesb.AiClassificator.exception;

import lombok.Getter;

/**
 * Excecao personalizada para erros de classificacao.
 */
@Getter
public class ClassificationException extends RuntimeException {

    private final String errorCode;
    private final String correlationId;

    public ClassificationException(String message) {
        super(message);
        this.errorCode = "CLASSIFICATION_ERROR";
        this.correlationId = null;
    }

    public ClassificationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.correlationId = null;
    }

    public ClassificationException(String message, String errorCode, String correlationId) {
        super(message);
        this.errorCode = errorCode;
        this.correlationId = correlationId;
    }

    public ClassificationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "CLASSIFICATION_ERROR";
        this.correlationId = null;
    }

    public ClassificationException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.correlationId = null;
    }
}
