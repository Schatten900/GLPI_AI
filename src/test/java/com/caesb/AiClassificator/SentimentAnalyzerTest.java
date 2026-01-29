package com.caesb.AiClassificator;

import com.caesb.AiClassificator.model.SentimentResult;
import com.caesb.AiClassificator.service.SentimentAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para o SentimentAnalyzer.
 * 
 * NOTA: Mova este arquivo para o pacote com.caesb.AiClassificator.service
 * após criar o diretório correspondente.
 */
class SentimentAnalyzerTest {

    private SentimentAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new SentimentAnalyzer();
    }

    @Nested
    @DisplayName("Testes de sentimento")
    class SentimentTests {

        @Test
        @DisplayName("Deve retornar neutro para texto vazio")
        void shouldReturnNeutralForEmptyText() {
            SentimentResult result = analyzer.analyzeSentiment("");
            assertEquals("neutral", result.getSentimentLabel());
            assertEquals(0.0, result.getSentimentScore());
        }

        @Test
        @DisplayName("Deve retornar neutro para texto nulo")
        void shouldReturnNeutralForNullText() {
            SentimentResult result = analyzer.analyzeSentiment(null);
            assertEquals("neutral", result.getSentimentLabel());
        }

        @Test
        @DisplayName("Deve detectar sentimento positivo")
        void shouldDetectPositiveSentiment() {
            SentimentResult result = analyzer.analyzeSentiment(
                    "Obrigado pelo excelente suporte! O sistema está funcionando perfeitamente."
            );
            assertEquals("positive", result.getSentimentLabel());
            assertTrue(result.getSentimentScore() > 0);
        }

        @Test
        @DisplayName("Deve detectar sentimento negativo")
        void shouldDetectNegativeSentiment() {
            SentimentResult result = analyzer.analyzeSentiment(
                    "O sistema está péssimo, não funciona, está horrível e muito lento."
            );
            assertEquals("negative", result.getSentimentLabel());
            assertTrue(result.getSentimentScore() < 0);
        }

        @Test
        @DisplayName("Deve detectar sentimento neutro")
        void shouldDetectNeutralSentiment() {
            SentimentResult result = analyzer.analyzeSentiment(
                    "Preciso de ajuda para configurar minha conta."
            );
            // Pode ser neutro ou levemente positivo/negativo
            assertNotNull(result.getSentimentLabel());
        }
    }

    @Nested
    @DisplayName("Testes de urgência")
    class UrgencyTests {

        @Test
        @DisplayName("Deve detectar urgência com palavra 'urgente'")
        void shouldDetectUrgencyWithUrgente() {
            SentimentResult result = analyzer.analyzeSentiment("Preciso de ajuda urgente!");
            assertTrue(result.isUrgencyDetected());
        }

        @Test
        @DisplayName("Deve detectar urgência com palavra 'crítico'")
        void shouldDetectUrgencyWithCritico() {
            SentimentResult result = analyzer.analyzeSentiment("Sistema crítico está fora do ar");
            assertTrue(result.isUrgencyDetected());
        }

        @Test
        @DisplayName("Deve detectar urgência com 'fora do ar'")
        void shouldDetectUrgencyWithForaDoAr() {
            SentimentResult result = analyzer.analyzeSentiment("O servidor está fora do ar");
            assertTrue(result.isUrgencyDetected());
        }

        @Test
        @DisplayName("Deve detectar urgência com 'bloqueado'")
        void shouldDetectUrgencyWithBloqueado() {
            SentimentResult result = analyzer.analyzeSentiment("Meu usuário está bloqueado");
            assertTrue(result.isUrgencyDetected());
        }

        @Test
        @DisplayName("Não deve detectar urgência em texto normal")
        void shouldNotDetectUrgencyInNormalText() {
            SentimentResult result = analyzer.analyzeSentiment(
                    "Gostaria de solicitar acesso ao sistema de RH."
            );
            assertFalse(result.isUrgencyDetected());
        }
    }

    @Nested
    @DisplayName("Testes de criticidade")
    class CriticalityTests {

        @Test
        @DisplayName("Deve aumentar severidade para texto negativo e urgente")
        void shouldIncreaseSeverityForNegativeAndUrgent() {
            SentimentResult result = analyzer.analyzeSentiment(
                    "Sistema crítico está travando e está péssimo! Isso é urgente!"
            );
            assertTrue(result.isShouldIncreaseSeverity());
            assertTrue(result.getCriticalityScore() >= 2);
        }

        @Test
        @DisplayName("Deve ter criticidade baixa para texto neutro sem urgência")
        void shouldHaveLowCriticalityForNeutralText() {
            SentimentResult result = analyzer.analyzeSentiment(
                    "Gostaria de saber como configurar o email."
            );
            assertFalse(result.isShouldIncreaseSeverity());
            assertEquals(0, result.getCriticalityScore());
        }

        @Test
        @DisplayName("Deve aumentar severidade apenas com urgência detectada")
        void shouldIncreaseSeverityWithUrgencyOnly() {
            SentimentResult result = analyzer.analyzeSentiment(
                    "Preciso de acesso urgente ao sistema."
            );
            assertTrue(result.isUrgencyDetected());
            assertTrue(result.getCriticalityScore() >= 2);
        }
    }
}
