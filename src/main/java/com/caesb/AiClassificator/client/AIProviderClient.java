package com.caesb.AiClassificator.client;

import com.caesb.AiClassificator.model.AIRequest;
import com.caesb.AiClassificator.model.AIResponse;

/**
 * Interface para clientes de provedores de IA (OpenAI, Claude, Gemini, etc).
 */
public interface AIProviderClient {

    /**
     * Envia uma requisicao de chat completion para a IA.
     *
     * @param request Requisicao com prompts e configuracoes
     * @return Resposta da IA
     */
    AIResponse sendChatCompletion(AIRequest request);

    /**
     * Retorna o nome do provider.
     */
    String getProviderName();

    /**
     * Verifica se o provider esta disponivel.
     */
    boolean isAvailable();

}
