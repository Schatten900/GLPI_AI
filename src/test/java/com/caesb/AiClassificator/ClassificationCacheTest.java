package com.caesb.AiClassificator;

import com.caesb.AiClassificator.model.ClassificationResponse;
import com.caesb.AiClassificator.service.ClassificationCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para o ClassificationCache.
 */
class ClassificationCacheTest {

    private ClassificationCache cache;

    @BeforeEach
    void setUp() {
        cache = new ClassificationCache();
        // Configura valores via reflection (simula @Value)
        ReflectionTestUtils.setField(cache, "ttlMinutes", 5);
        ReflectionTestUtils.setField(cache, "maxSize", 100);
    }

    @Nested
    @DisplayName("Testes de cache básico")
    class BasicCacheTests {

        @Test
        @DisplayName("Deve retornar empty para chave não existente")
        void shouldReturnEmptyForNonExistentKey() {
            Optional<ClassificationResponse> result = cache.get("ticket1", "subject", "body");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Deve armazenar e recuperar resposta")
        void shouldStoreAndRetrieveResponse() {
            ClassificationResponse response = ClassificationResponse.builder()
                    .success(true)
                    .status("applied")
                    .serviceId("REQ-101")
                    .serviceName("Resetar Senha")
                    .queue("Identidade e Acesso")
                    .build();

            cache.put("ticket1", "Problema com senha", "Não consigo acessar", response);

            Optional<ClassificationResponse> result = cache.get("ticket1", "Problema com senha", "Não consigo acessar");
            assertTrue(result.isPresent());
            assertEquals("REQ-101", result.get().getServiceId());
        }

        @Test
        @DisplayName("Deve retornar empty para subject diferente")
        void shouldReturnEmptyForDifferentSubject() {
            ClassificationResponse response = ClassificationResponse.builder()
                    .success(true)
                    .status("applied")
                    .build();

            cache.put("ticket1", "Subject A", "Body", response);

            Optional<ClassificationResponse> result = cache.get("ticket1", "Subject B", "Body");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Deve retornar empty para body diferente")
        void shouldReturnEmptyForDifferentBody() {
            ClassificationResponse response = ClassificationResponse.builder()
                    .success(true)
                    .status("applied")
                    .build();

            cache.put("ticket1", "Subject", "Body A", response);

            Optional<ClassificationResponse> result = cache.get("ticket1", "Subject", "Body B");
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Testes de geração de chave")
    class KeyGenerationTests {

        @Test
        @DisplayName("Deve gerar chave consistente para mesmos inputs")
        void shouldGenerateConsistentKey() throws Exception {
            String key1 = invokeGenerateKey("ticket1", "subject", "body");
            String key2 = invokeGenerateKey("ticket1", "subject", "body");
            assertEquals(key1, key2);
        }

        @Test
        @DisplayName("Deve gerar chaves diferentes para inputs diferentes")
        void shouldGenerateDifferentKeys() throws Exception {
            String key1 = invokeGenerateKey("ticket1", "subject", "body");
            String key2 = invokeGenerateKey("ticket2", "subject", "body");
            assertNotEquals(key1, key2);
        }

        @Test
        @DisplayName("Deve tratar valores nulos")
        void shouldHandleNullValues() {
            assertDoesNotThrow(() -> invokeGenerateKey(null, "subject", "body"));
            assertDoesNotThrow(() -> invokeGenerateKey("ticket", null, "body"));
            assertDoesNotThrow(() -> invokeGenerateKey("ticket", "subject", null));
        }

        private String invokeGenerateKey(String ticketId, String subject, String body) throws Exception {
            Method method = ClassificationCache.class.getDeclaredMethod(
                    "generateKey", String.class, String.class, String.class);
            method.setAccessible(true);
            return (String) method.invoke(cache, ticketId, subject, body);
        }
    }

    @Nested
    @DisplayName("Testes de estatísticas")
    class StatisticsTests {

        @Test
        @DisplayName("Deve retornar tamanho zero inicialmente")
        void shouldReturnZeroSizeInitially() {
            assertEquals(0, cache.size());
        }

        @Test
        @DisplayName("Deve incrementar tamanho ao adicionar itens")
        void shouldIncrementSizeWhenAddingItems() {
            ClassificationResponse response = ClassificationResponse.builder()
                    .success(true)
                    .build();

            cache.put("ticket1", "subject1", "body1", response);
            assertEquals(1, cache.size());

            cache.put("ticket2", "subject2", "body2", response);
            assertEquals(2, cache.size());
        }

        @Test
        @DisplayName("Deve limpar cache")
        void shouldClearCache() {
            ClassificationResponse response = ClassificationResponse.builder()
                    .success(true)
                    .build();

            cache.put("ticket1", "subject1", "body1", response);
            cache.put("ticket2", "subject2", "body2", response);
            assertEquals(2, cache.size());

            cache.clear();
            assertEquals(0, cache.size());
        }
    }
}
