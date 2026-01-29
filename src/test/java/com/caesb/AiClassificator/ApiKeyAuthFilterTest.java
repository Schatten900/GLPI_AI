package com.caesb.AiClassificator;

import com.caesb.AiClassificator.config.ApiKeyAuthFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

/**
 * Testes unitários para o ApiKeyAuthFilter.
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyAuthFilterTest {

    private ApiKeyAuthFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new ApiKeyAuthFilter();
        ReflectionTestUtils.setField(filter, "apiKey", "test-api-key-12345");
    }

    @Nested
    @DisplayName("Testes de endpoints públicos")
    class PublicEndpointTests {

        @Test
        @DisplayName("Deve permitir acesso a /api/v1/health sem API Key")
        void shouldAllowHealthEndpoint() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/v1/health");

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(response, never()).setStatus(anyInt());
        }

        @Test
        @DisplayName("Deve permitir acesso a /api/v1/catalog/services sem API Key")
        void shouldAllowCatalogEndpoint() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/v1/catalog/services");

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Deve permitir acesso a /actuator/health sem API Key")
        void shouldAllowActuatorHealth() throws Exception {
            when(request.getRequestURI()).thenReturn("/actuator/health");

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Deve permitir acesso a /swagger-ui sem API Key")
        void shouldAllowSwaggerUi() throws Exception {
            when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Deve permitir acesso a /v3/api-docs sem API Key")
        void shouldAllowApiDocs() throws Exception {
            when(request.getRequestURI()).thenReturn("/v3/api-docs");

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Testes de endpoints protegidos")
    class ProtectedEndpointTests {

        @Test
        @DisplayName("Deve permitir acesso com API Key válida")
        void shouldAllowWithValidApiKey() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/v1/classify");
            when(request.getHeader("X-API-Key")).thenReturn("test-api-key-12345");

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(response, never()).setStatus(anyInt());
        }

        @Test
        @DisplayName("Deve bloquear acesso sem API Key")
        void shouldBlockWithoutApiKey() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/v1/classify");
            when(request.getHeader("X-API-Key")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("192.168.1.100");
            when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

            filter.doFilter(request, response, filterChain);

            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
        }

        @Test
        @DisplayName("Deve bloquear acesso com API Key inválida")
        void shouldBlockWithInvalidApiKey() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/v1/classify");
            when(request.getHeader("X-API-Key")).thenReturn("chave-errada");
            when(request.getRemoteAddr()).thenReturn("192.168.1.100");
            when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

            filter.doFilter(request, response, filterChain);

            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
        }
    }

    @Nested
    @DisplayName("Testes de modo desenvolvimento")
    class DevModeTests {

        @Test
        @DisplayName("Deve permitir tudo quando API Key não configurada (modo dev)")
        void shouldAllowAllWhenApiKeyNotConfigured() throws Exception {
            ReflectionTestUtils.setField(filter, "apiKey", "");

            when(request.getRequestURI()).thenReturn("/api/v1/classify");
            when(request.getHeader("X-API-Key")).thenReturn(null);

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Deve permitir tudo quando API Key é null")
        void shouldAllowAllWhenApiKeyIsNull() throws Exception {
            ReflectionTestUtils.setField(filter, "apiKey", null);

            when(request.getRequestURI()).thenReturn("/api/v1/classify");

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Testes de endpoints admin")
    class AdminEndpointTests {

        @Test
        @DisplayName("Endpoint admin nunca é público")
        void adminEndpointNeverPublic() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/v1/admin/rotate-key");
            when(request.getHeader("X-API-Key")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("192.168.1.100");
            when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

            filter.doFilter(request, response, filterChain);

            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }

        @Test
        @DisplayName("Endpoint admin requer API Key")
        void adminEndpointRequiresApiKey() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/v1/admin/config-status");
            when(request.getHeader("X-API-Key")).thenReturn("test-api-key-12345");

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }
}
