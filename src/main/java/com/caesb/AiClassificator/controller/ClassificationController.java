package com.caesb.AiClassificator.controller;

import com.caesb.AiClassificator.model.*;
import com.caesb.AiClassificator.service.ClassificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Classification", description = "Endpoints para classificacao de tickets com IA")
public class ClassificationController {

    private final ClassificationService classificationService;

    /**
     * Classifica um ticket usando IA.
     */
    @Operation(
            summary = "Classificar ticket",
            description = "Classifica um ticket de Service Desk usando IA para determinar servico, fila e tipo",
            security = @SecurityRequirement(name = "apiKey")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classificacao processada (sucesso ou falha no body)",
                    content = @Content(schema = @Schema(implementation = ClassificationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request invalida (validacao)",
                    content = @Content(schema = @Schema(implementation = ClassificationResponse.class))),
            @ApiResponse(responseCode = "401", description = "API Key invalida ou ausente")
    })
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
     */
    @Operation(summary = "Listar servicos", description = "Retorna todos os servicos do catalogo CAESB")
    @ApiResponse(responseCode = "200", description = "Lista de servicos")
    @GetMapping("/catalog/services")
    public ResponseEntity<Map<String, Service>> getServices() {
        return ResponseEntity.ok(ServiceCatalog.getAllServices());
    }

    /**
     * Retorna todas as filas do catalogo.
     */
    @Operation(summary = "Listar filas", description = "Retorna todas as filas de atendimento do catalogo")
    @ApiResponse(responseCode = "200", description = "Lista de filas")
    @GetMapping("/catalog/queues")
    public ResponseEntity<Map<String, Queue>> getQueues() {
        return ResponseEntity.ok(ServiceCatalog.getAllQueues());
    }

    /**
     * Retorna informacoes de um servico especifico.
     */
    @Operation(summary = "Buscar servico", description = "Retorna informacoes de um servico especifico pelo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Servico encontrado",
                    content = @Content(schema = @Schema(implementation = Service.class))),
            @ApiResponse(responseCode = "404", description = "Servico nao encontrado")
    })
    @GetMapping("/catalog/services/{serviceId}")
    public ResponseEntity<Service> getService(
            @Parameter(description = "ID do servico (ex: REQ-101)") @PathVariable String serviceId) {
        Service service = ServiceCatalog.getService(serviceId);
        if (service != null) {
            return ResponseEntity.ok(service);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Endpoint de health check.
     */
    @Operation(summary = "Health check", description = "Verifica se o servico esta operacional")
    @ApiResponse(responseCode = "200", description = "Servico operacional")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "AiClassificator",
                "version", "1.0.0"
        ));
    }
}
