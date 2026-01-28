package com.caesb.AiClassificator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuracao de seguranca do Spring Security.
 * Por padrao, permite todas as requisicoes para a API.
 * Em producao, deve ser configurado com autenticacao adequada.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Desabilita CSRF para API REST
                .csrf(csrf -> csrf.disable())

                // Stateless - nao usa sessao
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configuracao de autorizacao
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publicos
                        .requestMatchers("/api/v1/health").permitAll()
                        .requestMatchers("/api/v1/catalog/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // Endpoint de classificacao - pode ser protegido em producao
                        .requestMatchers("/api/v1/classify").permitAll()

                        // Qualquer outra requisicao requer autenticacao
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
