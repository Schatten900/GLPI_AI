package com.caesb.AiClassificator.controller;

import com.caesb.AiClassificator.client.AIProviderFactory;
import com.caesb.AiClassificator.client.AIProviderRegistry;
import com.caesb.AiClassificator.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller para gerenciamento de providers de IA.
 * Permite listar providers/modelos disponiveis e testar conectividade.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
@Tag(name = "Providers", description = "Gerenciamento de providers e modelos de IA")
public class ProviderController {

    private final AIProviderRegistry registry;
    private final AIProviderFactory factory;

    /**
     * Lista todos os providers e modelos disponiveis.
     * Endpoint para o GLPI descobrir opcoes de configuracao.
     */
    @Operation(
            summary = "Listar providers",
            description = "Lista todos os providers e modelos de IA disponiveis para classificacao",
            security = @SecurityRequirement(name = "apiKey")
    )
    @ApiResponse(responseCode = "200", description = "Lista de providers",
            content = @Content(schema = @Schema(implementation = ProvidersListResponse.class)))
    @GetMapping
    public ResponseEntity<ProvidersListResponse> listProviders() {
        log.info("Listando providers disponiveis");

        List<ProviderInfo> providers = new ArrayList<>();

        registry.getProviderDeployments().forEach((providerId, deployments) -> {
            List<ProviderInfo.ModelInfo> models = deployments.stream()
                    .map(d -> ProviderInfo.ModelInfo.builder()
                            .id(d.getModelId())
                            .displayName(d.getDisplayName())
                            .description(d.getDescription())
                            .enabled(d.isEnabled())
                            .build())
                    .toList();

            ProviderInfo provider = ProviderInfo.builder()
                    .id(providerId)
                    .displayName(getProviderDisplayName(providerId))
                    .enabled(!models.isEmpty())
                    .models(models)
                    .build();

            providers.add(provider);
        });

        ProvidersListResponse response = ProvidersListResponse.builder()
                .providers(providers)
                .defaultProvider(registry.getDefaultProvider())
                .defaultModel(registry.getDefaultModel())
                .totalModels(registry.getTotalModelsCount())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Testa conectividade com um provider/modelo especifico.
     */
    @Operation(
            summary = "Testar conexao",
            description = "Testa conectividade com um provider/modelo especifico do Azure OpenAI",
            security = @SecurityRequirement(name = "apiKey")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado do teste",
                    content = @Content(schema = @Schema(implementation = TestConnectionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Provider ou modelo invalido")
    })
    @PostMapping("/test")
    public ResponseEntity<TestConnectionResponse> testConnection(@RequestBody TestConnectionRequest request) {
        String provider = request.getProvider() != null ? request.getProvider() : registry.getDefaultProvider();
        String model = request.getModel() != null ? request.getModel() : registry.getDefaultModel();

        log.info("Testando conexao com provider: {}, model: {}", provider, model);

        // Valida se provider existe
        if (!registry.isProviderAvailable(provider)) {
            return ResponseEntity.badRequest().body(
                    TestConnectionResponse.builder()
                            .success(false)
                            .provider(provider)
                            .model(model)
                            .errorCode("PROVIDER_NOT_FOUND")
                            .errorMessage("Provider '" + provider + "' nao encontrado ou desabilitado")
                            .build()
            );
        }

        // Valida se modelo existe no provider
        if (!registry.isModelAvailable(provider, model)) {
            return ResponseEntity.badRequest().body(
                    TestConnectionResponse.builder()
                            .success(false)
                            .provider(provider)
                            .model(model)
                            .errorCode("MODEL_NOT_FOUND")
                            .errorMessage("Modelo '" + model + "' nao encontrado no provider '" + provider + "'")
                            .build()
            );
        }

        // Executa teste de conexao
        AIResponse aiResponse = factory.testConnection(provider, model);

        TestConnectionResponse response = TestConnectionResponse.builder()
                .success(aiResponse.isSuccess())
                .provider(provider)
                .model(model)
                .latencyMs(aiResponse.getLatencyMs())
                .message(aiResponse.isSuccess()
                        ? "Conexao estabelecida com sucesso"
                        : "Falha na conexao")
                .errorCode(aiResponse.getErrorCode())
                .errorMessage(aiResponse.getErrorMessage())
                .build();

        if (aiResponse.isSuccess()) {
            log.info("Teste de conexao bem sucedido - provider: {}, model: {}, latency: {}ms",
                    provider, model, aiResponse.getLatencyMs());
            return ResponseEntity.ok(response);
        } else {
            log.warn("Teste de conexao falhou - provider: {}, model: {}, erro: {}",
                    provider, model, aiResponse.getErrorMessage());
            return ResponseEntity.ok(response);  // Retorna 200 mesmo com falha, erro vai no body
        }
    }

    /**
     * Retorna nome de exibicao para um provider.
     */
    private String getProviderDisplayName(String providerId) {
        return switch (providerId) {
            case "azure-openai" -> "Azure OpenAI";
            default -> providerId;
        };
    }
}
