package com.caesb.AiClassificator.service;

import com.caesb.AiClassificator.model.SanitizedData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Servico de sanitizacao de dados de tickets antes de enviar para a IA.
 * Remove PII (Informacoes Pessoais Identificaveis), mascara emails, CPF, telefones.
 */
@Slf4j
@Service
public class Sanitizer {

    @Value("${ai.sanitizer.body-max-length:300}")
    private int bodyMaxLength;

    @Value("${ai.sanitizer.body-min-length:200}")
    private int bodyMinLength;

    @Value("${ai.sanitizer.sanitize-pii:true}")
    private boolean sanitizePii;

    // Patterns para deteccao de PII
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CPF_PATTERN = Pattern.compile(
            "\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}"
    );

    private static final Pattern CNPJ_PATTERN = Pattern.compile(
            "\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\+?\\d{1,3}[-.\\s]?\\(?\\d{2,3}\\)?[-.\\s]?\\d{4,5}[-.\\s]?\\d{4}"
    );

    private static final Pattern PHONE_BR_PATTERN = Pattern.compile(
            "\\(\\d{2}\\)\\s*\\d{4,5}-?\\d{4}"
    );

    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
            "\\d{4}[-.\\s]?\\d{4}[-.\\s]?\\d{4}[-.\\s]?\\d{4}"
    );

    private static final Pattern IP_PATTERN = Pattern.compile(
            "\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b"
    );

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    // Patterns para remover assinaturas de email
    private static final List<Pattern> SIGNATURE_PATTERNS = Arrays.asList(
            Pattern.compile("--\\s*\\n.*", Pattern.DOTALL),
            Pattern.compile("_{3,}.*", Pattern.DOTALL),
            Pattern.compile("Enviado do meu.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("Sent from my.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("Esta mensagem.*confidencial.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("Atenciosamente,?\\s*\\n.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("Att,?\\s*\\n.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
    );

    // Patterns para remover prefixos de subject
    private static final Pattern TICKET_NUMBER_PATTERN = Pattern.compile(
            "\\[Ticket#\\d+\\]\\s*|Ticket#\\d+\\s*:?\\s*",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern REPLY_FWD_PATTERN = Pattern.compile(
            "^(Re|Fw|Fwd|Enc|RES|ENC):\\s*",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Sanitiza o corpo do ticket para envio a IA.
     * Remove HTML, PII, assinaturas e trunca para o tamanho maximo.
     *
     * @param body Corpo original do ticket
     * @return Corpo sanitizado
     */
    public String sanitizeBody(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }

        String sanitized = body;

        // Remove tags HTML
        sanitized = HTML_TAG_PATTERN.matcher(sanitized).replaceAll("");

        // Remove excesso de whitespace
        sanitized = WHITESPACE_PATTERN.matcher(sanitized).replaceAll(" ");
        sanitized = sanitized.trim();

        // Remove assinaturas de email
        for (Pattern pattern : SIGNATURE_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll("");
        }

        // Remove PII se habilitado
        if (sanitizePii) {
            sanitized = removePii(sanitized);
        }

        // Trunca para o tamanho maximo, tentando cortar em limite de palavra
        if (sanitized.length() > bodyMaxLength) {
            sanitized = sanitized.substring(0, bodyMaxLength);
            // Tenta cortar na ultima palavra completa
            int lastSpace = sanitized.lastIndexOf(' ');
            if (lastSpace > bodyMinLength) {
                sanitized = sanitized.substring(0, lastSpace);
            }
            sanitized = sanitized + "...";
        }

        return sanitized.trim();
    }

    /**
     * Sanitiza o assunto do ticket para envio a IA.
     * Remove numero do ticket e prefixos RE/FW.
     *
     * @param subject Assunto original
     * @return Assunto sanitizado
     */
    public String sanitizeSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return "";
        }

        String sanitized = subject;

        // Remove numero do ticket
        sanitized = TICKET_NUMBER_PATTERN.matcher(sanitized).replaceAll("");

        // Remove prefixos RE:/FW:
        sanitized = REPLY_FWD_PATTERN.matcher(sanitized).replaceAll("");

        // Remove PII se habilitado
        if (sanitizePii) {
            sanitized = removePii(sanitized);
        }

        return sanitized.trim();
    }

    /**
     * Mascara um endereco de email para privacidade.
     * Exemplo: john.doe@example.com -> j****@example.com
     *
     * @param email Email original
     * @return Email mascarado
     */
    public String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }

        String emailToMask = email;

        // Extrai email de formato "Nome <email@domain.com>"
        if (email.contains("<") && email.contains(">")) {
            int start = email.indexOf('<');
            int end = email.indexOf('>');
            if (start < end) {
                emailToMask = email.substring(start + 1, end);
            }
        }

        // Mascara a parte local do email
        int atIndex = emailToMask.indexOf('@');
        if (atIndex > 0) {
            String firstChar = emailToMask.substring(0, 1);
            String domain = emailToMask.substring(atIndex);
            return firstChar + "****" + domain;
        }

        return emailToMask;
    }

    /**
     * Remove informacoes pessoais identificaveis (PII) do texto.
     *
     * @param text Texto original
     * @return Texto com PII removido
     */
    public String removePii(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String sanitized = text;

        // Mascara emails
        sanitized = EMAIL_PATTERN.matcher(sanitized).replaceAll("[EMAIL]");

        // Mascara telefones
        sanitized = PHONE_PATTERN.matcher(sanitized).replaceAll("[PHONE]");
        sanitized = PHONE_BR_PATTERN.matcher(sanitized).replaceAll("[PHONE]");

        // Mascara CPF
        sanitized = CPF_PATTERN.matcher(sanitized).replaceAll("[CPF]");

        // Mascara CNPJ
        sanitized = CNPJ_PATTERN.matcher(sanitized).replaceAll("[CNPJ]");

        // Mascara cartoes de credito
        sanitized = CREDIT_CARD_PATTERN.matcher(sanitized).replaceAll("[CARD]");

        // Mascara IPs
        sanitized = IP_PATTERN.matcher(sanitized).replaceAll("[IP]");

        return sanitized;
    }

    /**
     * Sanitiza todos os dados do ticket em uma unica chamada.
     *
     * @param subject Assunto do ticket
     * @param body    Corpo do ticket
     * @param sender  Email do remetente
     * @return SanitizedData com os dados sanitizados
     */
    public SanitizedData sanitizeAll(String subject, String body, String sender) {
        return SanitizedData.builder()
                .subject(sanitizeSubject(subject))
                .body(sanitizeBody(body))
                .maskedSender(maskEmail(sender))
                .build();
    }

}
