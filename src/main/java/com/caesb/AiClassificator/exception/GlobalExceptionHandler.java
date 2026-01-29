package com.caesb.AiClassificator.exception;

import com.caesb.AiClassificator.model.ClassificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Handler global de excecoes para a API REST.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Trata erros de validacao de request.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ClassificationResponse> handleValidationException(
            MethodArgumentNotValidException ex) {

        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Erro de validacao: {}", errors);

        return ResponseEntity.badRequest().body(
                ClassificationResponse.builder()
                        .success(false)
                        .status("not_applied")
                        .errorCode("VALIDATION_ERROR")
                        .errorMessage(errors)
                        .build()
        );
    }

    /**
     * Trata excecoes de classificacao.
     */
    @ExceptionHandler(ClassificationException.class)
    public ResponseEntity<ClassificationResponse> handleClassificationException(
            ClassificationException ex) {

        // Log com stack trace para facilitar debug em producao
        log.error("Erro de classificacao: {} - {} [correlationId={}]",
                ex.getErrorCode(), ex.getMessage(), ex.getCorrelationId(), ex);

        return ResponseEntity.ok(
                ClassificationResponse.builder()
                        .success(false)
                        .status("not_applied")
                        .correlationId(ex.getCorrelationId())
                        .errorCode(ex.getErrorCode())
                        .errorMessage(ex.getMessage())
                        .build()
        );
    }

    /**
     * Trata excecoes genericas.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ClassificationResponse> handleGenericException(Exception ex) {
        log.error("Erro inesperado: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ClassificationResponse.builder()
                        .success(false)
                        .status("not_applied")
                        .errorCode("INTERNAL_ERROR")
                        .errorMessage("Erro interno: " + ex.getMessage())
                        .build()
        );
    }
}
