package com.caesb.AiClassificator.service;

import com.caesb.AiClassificator.model.ClassificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache em memória para respostas de classificação.
 * Implementa idempotência para evitar reprocessamento de tickets idênticos.
 */
@Slf4j
@Component
public class ClassificationCache {

    // Cache: chave = hash(ticketId + subject + body), valor = resposta + timestamp
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Value("${ai.cache.ttl-minutes:5}")
    private int ttlMinutes;

    @Value("${ai.cache.max-size:1000}")
    private int maxSize;

    /**
     * Busca resposta em cache para um ticket.
     *
     * @param ticketId ID do ticket (pode ser null)
     * @param subject  Assunto do ticket
     * @param body     Corpo do ticket
     * @return Resposta em cache se existir e não expirada
     */
    public Optional<ClassificationResponse> get(String ticketId, String subject, String body) {
        String key = generateKey(ticketId, subject, body);
        CacheEntry entry = cache.get(key);

        if (entry == null) {
            return Optional.empty();
        }

        // Verifica se expirou
        long ttlMs = ttlMinutes * 60 * 1000L;
        if (System.currentTimeMillis() - entry.timestamp > ttlMs) {
            cache.remove(key);
            log.debug("Cache expirado para key: {}", key.substring(0, 8));
            return Optional.empty();
        }

        log.debug("Cache hit para ticket: {}", ticketId != null ? ticketId : "N/A");
        return Optional.of(entry.response);
    }

    /**
     * Armazena resposta no cache.
     *
     * @param ticketId ID do ticket (pode ser null)
     * @param subject  Assunto do ticket
     * @param body     Corpo do ticket
     * @param response Resposta a armazenar
     */
    public void put(String ticketId, String subject, String body, ClassificationResponse response) {
        // Limpa cache se muito grande
        if (cache.size() >= maxSize) {
            evictOldEntries();
        }

        String key = generateKey(ticketId, subject, body);
        cache.put(key, new CacheEntry(response, System.currentTimeMillis()));
        log.debug("Cache armazenado para ticket: {}, key: {}",
                ticketId != null ? ticketId : "N/A", key.substring(0, 8));
    }

    /**
     * Retorna o tamanho atual do cache.
     */
    public int size() {
        return cache.size();
    }

    /**
     * Retorna estatísticas do cache.
     */
    public Map<String, Object> getStats() {
        return Map.of(
                "size", cache.size(),
                "maxSize", maxSize,
                "ttlMinutes", ttlMinutes
        );
    }

    /**
     * Limpa todo o cache.
     */
    public void clear() {
        cache.clear();
        log.info("Cache limpo manualmente");
    }

    /**
     * Gera chave única baseada no conteúdo do ticket.
     * Visibilidade package-private para testes.
     */
    public String generateKey(String ticketId, String subject, String body) {
        String content = (ticketId != null ? ticketId : "") + "|" +
                (subject != null ? subject : "") + "|" +
                (body != null ? body : "");
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().substring(0, 32); // Primeiros 32 chars
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(content.hashCode());
        }
    }

    /**
     * Remove entradas expiradas.
     */
    private void evictOldEntries() {
        long now = System.currentTimeMillis();
        long ttlMs = ttlMinutes * 60 * 1000L;

        int sizeBefore = cache.size();
        cache.entrySet().removeIf(e -> now - e.getValue().timestamp > ttlMs);

        int removed = sizeBefore - cache.size();
        if (removed > 0) {
            log.debug("Removidas {} entradas expiradas do cache", removed);
        }

        // Se ainda muito grande, remove metade das entradas mais antigas
        if (cache.size() >= maxSize) {
            log.warn("Cache ainda cheio apos limpeza, removendo entradas extras");
            int toRemove = cache.size() / 2;
            cache.keySet().stream()
                    .limit(toRemove)
                    .toList()
                    .forEach(cache::remove);
        }
    }

    /**
     * Entrada do cache com timestamp.
     */
    private record CacheEntry(ClassificationResponse response, long timestamp) {
    }
}
