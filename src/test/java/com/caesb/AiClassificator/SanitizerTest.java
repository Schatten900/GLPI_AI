package com.caesb.AiClassificator;

import com.caesb.AiClassificator.service.Sanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para o Sanitizer.
 * 
 * NOTA: Mova este arquivo para o pacote com.caesb.AiClassificator.service
 * após criar o diretório correspondente.
 */
class SanitizerTest {

    private Sanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new Sanitizer();
        // Configura valores via reflection (simula @Value)
        ReflectionTestUtils.setField(sanitizer, "bodyMaxLength", 300);
        ReflectionTestUtils.setField(sanitizer, "bodyMinLength", 200);
        ReflectionTestUtils.setField(sanitizer, "sanitizePii", true);
    }

    @Nested
    @DisplayName("Testes de sanitizeBody")
    class SanitizeBodyTests {

        @Test
        @DisplayName("Deve retornar string vazia para body nulo")
        void shouldReturnEmptyForNullBody() {
            assertEquals("", sanitizer.sanitizeBody(null));
        }

        @Test
        @DisplayName("Deve retornar string vazia para body em branco")
        void shouldReturnEmptyForBlankBody() {
            assertEquals("", sanitizer.sanitizeBody("   "));
        }

        @Test
        @DisplayName("Deve remover tags HTML")
        void shouldRemoveHtmlTags() {
            String input = "<html><body><p>Texto <strong>importante</strong></p></body></html>";
            String result = sanitizer.sanitizeBody(input);
            assertFalse(result.contains("<"));
            assertFalse(result.contains(">"));
            assertTrue(result.contains("Texto"));
            assertTrue(result.contains("importante"));
        }

        @Test
        @DisplayName("Deve normalizar whitespace excessivo")
        void shouldNormalizeWhitespace() {
            String input = "Texto    com   muito     espaco";
            String result = sanitizer.sanitizeBody(input);
            assertEquals("Texto com muito espaco", result);
        }

        @Test
        @DisplayName("Deve truncar texto longo")
        void shouldTruncateLongText() {
            String input = "a".repeat(500);
            String result = sanitizer.sanitizeBody(input);
            assertTrue(result.length() <= 303); // 300 + "..."
            assertTrue(result.endsWith("..."));
        }
    }

    @Nested
    @DisplayName("Testes de removePii")
    class RemovePiiTests {

        @Test
        @DisplayName("Deve mascarar emails")
        void shouldMaskEmails() {
            String input = "Contato: joao.silva@caesb.df.gov.br e maria@gmail.com";
            String result = sanitizer.removePii(input);
            assertTrue(result.contains("[EMAIL]"));
            assertFalse(result.contains("joao.silva@caesb.df.gov.br"));
            assertFalse(result.contains("maria@gmail.com"));
        }

        @Test
        @DisplayName("Deve mascarar CPF com pontuacao")
        void shouldMaskCpfWithPunctuation() {
            String input = "CPF do usuario: 123.456.789-01";
            String result = sanitizer.removePii(input);
            assertTrue(result.contains("[CPF]"));
            assertFalse(result.contains("123.456.789-01"));
        }

        @Test
        @DisplayName("Deve mascarar CPF sem pontuacao")
        void shouldMaskCpfWithoutPunctuation() {
            String input = "CPF: 12345678901";
            String result = sanitizer.removePii(input);
            assertTrue(result.contains("[CPF]"));
            assertFalse(result.contains("12345678901"));
        }

        @Test
        @DisplayName("Deve mascarar telefones brasileiros")
        void shouldMaskBrazilianPhones() {
            String input = "Telefone: (61) 99999-9999 ou (11) 3333-4444";
            String result = sanitizer.removePii(input);
            assertTrue(result.contains("[PHONE]"));
            assertFalse(result.contains("99999-9999"));
        }

        @Test
        @DisplayName("Deve mascarar IPs")
        void shouldMaskIpAddresses() {
            String input = "Acesso do IP 192.168.1.100 negado";
            String result = sanitizer.removePii(input);
            assertTrue(result.contains("[IP]"));
            assertFalse(result.contains("192.168.1.100"));
        }

        @Test
        @DisplayName("Deve mascarar cartoes de credito")
        void shouldMaskCreditCards() {
            String input = "Cartao: 1234-5678-9012-3456";
            String result = sanitizer.removePii(input);
            assertTrue(result.contains("[CARD]"));
            assertFalse(result.contains("1234-5678-9012-3456"));
        }
    }

    @Nested
    @DisplayName("Testes de sanitizeSubject")
    class SanitizeSubjectTests {

        @Test
        @DisplayName("Deve retornar string vazia para subject nulo")
        void shouldReturnEmptyForNullSubject() {
            assertEquals("", sanitizer.sanitizeSubject(null));
        }

        @Test
        @DisplayName("Deve remover numero do ticket")
        void shouldRemoveTicketNumber() {
            String input = "[Ticket#12345] Problema com senha";
            String result = sanitizer.sanitizeSubject(input);
            assertEquals("Problema com senha", result);
        }

        @Test
        @DisplayName("Deve remover prefixo RE:")
        void shouldRemoveRePrefix() {
            String input = "Re: Problema com VPN";
            String result = sanitizer.sanitizeSubject(input);
            assertEquals("Problema com VPN", result);
        }

        @Test
        @DisplayName("Deve remover prefixo FW:")
        void shouldRemoveFwPrefix() {
            String input = "Fw: Solicitacao de acesso";
            String result = sanitizer.sanitizeSubject(input);
            assertEquals("Solicitacao de acesso", result);
        }

        @Test
        @DisplayName("Deve remover prefixo RES:")
        void shouldRemoveResPrefix() {
            String input = "RES: Duvida sobre sistema";
            String result = sanitizer.sanitizeSubject(input);
            assertEquals("Duvida sobre sistema", result);
        }
    }

    @Nested
    @DisplayName("Testes de maskEmail")
    class MaskEmailTests {

        @Test
        @DisplayName("Deve mascarar email simples")
        void shouldMaskSimpleEmail() {
            String result = sanitizer.maskEmail("joao.silva@caesb.df.gov.br");
            assertEquals("j****@caesb.df.gov.br", result);
        }

        @Test
        @DisplayName("Deve mascarar email em formato Nome <email>")
        void shouldMaskEmailWithName() {
            String result = sanitizer.maskEmail("Joao Silva <joao.silva@caesb.df.gov.br>");
            assertEquals("j****@caesb.df.gov.br", result);
        }

        @Test
        @DisplayName("Deve retornar string vazia para email nulo")
        void shouldReturnEmptyForNullEmail() {
            assertEquals("", sanitizer.maskEmail(null));
        }

        @Test
        @DisplayName("Deve retornar string vazia para email em branco")
        void shouldReturnEmptyForBlankEmail() {
            assertEquals("", sanitizer.maskEmail("   "));
        }
    }
}
