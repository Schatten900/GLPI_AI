package com.caesb.AiClassificator.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;

/**
 * Configuracao programatica do Resilience4j.
 * Complementa as configuracoes do application.yml.
 */
@Configuration
public class ResilienceConfig {

    /**
     * Configuracao do CircuitBreaker para OpenAI.
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .slowCallDurationThreshold(Duration.ofSeconds(10))
                .slowCallRateThreshold(100)
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    /**
     * Configuracao do Retry para OpenAI.
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .retryExceptions(
                        RestClientException.class,
                        SocketTimeoutException.class,
                        IOException.class
                )
                .build();

        return RetryRegistry.of(config);
    }
}
