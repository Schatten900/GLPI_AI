package com.caesb.AiClassificator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuracao do RestTemplate e ObjectMapper.
 */
@Configuration
public class RestTemplateConfig {

    @Value("${ai.azure-openai.timeout:30000}")
    private int readTimeout;

    @Value("${ai.azure-openai.connect-timeout:5000}")
    private int connectTimeout;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);  // Timeout para estabelecer conexão
        factory.setReadTimeout(readTimeout);        // Timeout para ler resposta
        return new RestTemplate(factory);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
