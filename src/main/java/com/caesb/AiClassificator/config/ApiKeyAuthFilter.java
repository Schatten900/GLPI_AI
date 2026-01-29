package com.caesb.AiClassificator.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de autenticação por API Key.
 * Valida o header X-API-Key em requisições protegidas.
 */
@Slf4j
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    @Value("${security.api-key:}")
    private String apiKey;

    @Override
    public void doFilterInternal(HttpServletRequest request,
                                 HttpServletResponse response,
                                 FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Endpoints públicos não precisam de autenticação
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Se API Key não configurada, permite (modo dev)
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("API Key não configurada - modo desenvolvimento ativo");
            filterChain.doFilter(request, response);
            return;
        }

        // Valida API Key
        String requestApiKey = request.getHeader(API_KEY_HEADER);

        if (requestApiKey == null || !requestApiKey.equals(apiKey)) {
            log.warn("Tentativa de acesso não autorizado - path: {}, IP: {}",
                    path, request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"success\":false,\"error\":\"API Key invalida ou ausente\",\"errorCode\":\"UNAUTHORIZED\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Define quais endpoints são públicos (não requerem API Key).
     */
    private boolean isPublicEndpoint(String path) {
        // Endpoints admin NUNCA são públicos
        if (path.startsWith("/api/v1/admin")) {
            return false;
        }

        return path.equals("/api/v1/health")
                || path.startsWith("/api/v1/catalog/")
                || path.equals("/actuator/health")
                || path.equals("/actuator/health/liveness")
                || path.equals("/actuator/health/readiness")
                || path.equals("/actuator/info")
                // OpenAPI/Swagger endpoints
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/swagger-ui.html");
    }
}
