package com.caesb.AiClassificator.model;

import lombok.*;

/**
 * DTO para dados sanitizados.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SanitizedData {
    private String subject;
    private String body;
    private String maskedSender;
}
