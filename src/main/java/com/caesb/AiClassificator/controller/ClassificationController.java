package com.caesb.AiClassificator.controller;

import com.caesb.AiClassificator.model.ClassificationRequest;
import com.caesb.AiClassificator.model.ClassificationResponse;
import com.caesb.AiClassificator.model.ServiceCatalog;
import com.caesb.AiClassificator.service.ClassificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller REST para classificacao de tickets com IA.
 * Expoe endpoints para classificar tickets e consultar catalogo de servicos.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ClassificationController {

    private final ClassificationService classificationService;

    /**
     * Classifica um ticket usando IA.
     *
     * POST /api/v1/classify
     *
     * Request body:
     * {
     *   "subject": "Problema com senha",
     *   "body": "Nao consigo acessar minha conta...",
     *   "senderEmail": "usuario@caesb.df.gov.br",
     *   "ticketId": "12345"
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "status": "applied",
     *   "type": "REQ",
     *   "serviceId": "REQ-101",
     *   "serviceName": "Resetar Senha de Usuario",
     *   "queue": "Identidade e Acesso",
     *   "confidenceScore": 0.92,
     *   ...
     * }
     */
    @PostMapping("/classify")
    public ResponseEntity<ClassificationResponse> classify(
            @Valid @RequestBody ClassificationRequest request) {

        log.info("Recebida requisicao de classificacao - ticketId: {}",
                request.getTicketId() != null ? request.getTicketId() : "N/A");

        ClassificationResponse response = classificationService.classify(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            // Retorna 200 mesmo em caso de erro de classificacao,
            // pois a requisicao foi processada corretamente
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Retorna todos os servicos do catalogo.
     *
     * GET /api/v1/catalog/services
     */
    @GetMapping("/catalog/services")
    public ResponseEntity<Map<String, ServiceCatalog.Service>> getServices() {
        return ResponseEntity.ok(ServiceCatalog.getAllServices());
    }

    /**
     * Retorna todas as filas do catalogo.
     *
     * GET /api/v1/catalog/queues
     */
    @GetMapping("/catalog/queues")
    public ResponseEntity<Map<String, ServiceCatalog.Queue>> getQueues() {
        return ResponseEntity.ok(ServiceCatalog.getAllQueues());
    }

    /**
     * Retorna informacoes de um servico especifico.
     *
     * GET /api/v1/catalog/services/{serviceId}
     */
    @GetMapping("/catalog/services/{serviceId}")
    public ResponseEntity<ServiceCatalog.Service> getService(@PathVariable String serviceId) {
        ServiceCatalog.Service service = ServiceCatalog.getService(serviceId);
        if (service != null) {
            return ResponseEntity.ok(service);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Endpoint de health check.
     *
     * GET /api/v1/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "AiClassificator",
                "version", "1.0.0"
        ));
    }
}
