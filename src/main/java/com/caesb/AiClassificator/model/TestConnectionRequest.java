package com.caesb.AiClassificator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para testar conectividade com um provider/modelo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestConnectionRequest {

    /**
     * ID do provider (ex: azure-openai, openai).
     * Se nao informado, usa o default.
     */
    private String provider;

    /**
     * ID do modelo (ex: gpt-4o-mini, claude-3-sonnet).
     * Se nao informado, usa o default do provider.
     */
    private String model;
}
