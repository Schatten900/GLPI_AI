package com.caesb.AiClassificator.model;

import lombok.*;

/**
 * Resultado da construcao do prompt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PromptResult {
    private String systemPrompt;
    private String userPrompt;
}
