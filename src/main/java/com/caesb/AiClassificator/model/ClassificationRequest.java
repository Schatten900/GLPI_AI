package com.caesb.AiClassificator.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request model para classificacao de tickets com IA.
 * Recebe subject e body do ticket para classificacao.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationRequest {

    /**
     * Assunto do ticket (obrigatorio).
     * Deve ter no minimo 5 caracteres para classificacao adequada.
     */
    @NotBlank(message = "Subject e obrigatorio")
    @Size(min = 5, max = 500, message = "Subject deve ter entre 5 e 500 caracteres")
    private String subject;

    /**
     * Corpo do ticket (opcional, sera sanitizado e truncado).
     */
    @Size(max = 500, message = "Body deve ter no maximo 500 caracteres")
    private String body;

    /**
     * Email do remetente (opcional, sera mascarado).
     */
    private String senderEmail;

    /**
     * ID do ticket no sistema de origem (opcional, para correlacao).
     */
    private String ticketId;

    /**
     * Provider de IA a usar (OpenAI, Claude, Gemini). Opcional, usa default do config.
     */
    private String provider;

    /**
     * Modelo especifico a usar. Opcional, usa default do provider.
     */
    private String model;

    /**
     * ID de correlacao para rastreamento. Se nao fornecido, sera gerado.
     */
    private String correlationId;
}
