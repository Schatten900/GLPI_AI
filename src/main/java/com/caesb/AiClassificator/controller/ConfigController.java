package com.caesb.AiClassificator.controller;

import com.caesb.AiClassificator.client.AIProviderRegistry;
import com.caesb.AiClassificator.config.AIProviderConfig;
import com.caesb.AiClassificator.config.AzureOpenAIConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller para operações administrativas.
 * Todos os endpoints requerem autenticação via X-API-Key + X-Admin-Key.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Operações administrativas (requerem X-Admin-Key)")
public class ConfigController {

    private final AzureOpenAIConfig azureConfig;
    private final AIProviderConfig providerConfig;
    private final AIProviderRegistry registry;

    @Value("${security.admin-key:}")
    private String adminKey;

    /**
     * Atualiza a API Key do Azure em runtime.
     */
    @Operation(
            summary = "Rotacionar API Key",
            description = "Atualiza a API Key do Azure OpenAI em runtime sem necessidade de restart"
    )
    @SecurityRequirements({
            @SecurityRequirement(name = "apiKey"),
            @SecurityRequirement(name = "adminKey")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chave atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Campo apiKey ausente"),
            @ApiResponse(responseCode = "403", description = "Admin key invalida")
    })
    @PostMapping("/rotate-key")
    public ResponseEntity<Map<String, Object>> rotateApiKey(
            @RequestHeader(value = "X-Admin-Key", required = false) String requestAdminKey,
            @RequestBody Map<String, String> request) {

        ResponseEntity<Map<String, Object>> authError = validateAdminKey(requestAdminKey);
        if (authError != null) return authError;

        String newApiKey = request.get("apiKey");
        if (newApiKey == null || newApiKey.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "errorCode", "VALIDATION_ERROR",
                    "message", "Campo 'apiKey' é obrigatório"
            ));
        }

        // Atualiza a chave em memória
        azureConfig.setApiKey(newApiKey);

        log.info("API Key do Azure atualizada com sucesso");

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "API Key atualizada com sucesso"
        ));
    }

    /**
     * Altera o modelo padrão usado para classificação.
     */
    @Operation(
            summary = "Alterar modelo padrao",
            description = "Altera o modelo de IA usado por padrao quando nao especificado na requisicao"
    )
    @SecurityRequirements({
            @SecurityRequirement(name = "apiKey"),
            @SecurityRequirement(name = "adminKey")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Modelo alterado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Modelo invalido ou nao disponivel"),
            @ApiResponse(responseCode = "403", description = "Admin key invalida")
    })
    @PostMapping("/default-model")
    public ResponseEntity<Map<String, Object>> setDefaultModel(
            @RequestHeader(value = "X-Admin-Key", required = false) String requestAdminKey,
            @RequestBody Map<String, String> request) {

        ResponseEntity<Map<String, Object>> authError = validateAdminKey(requestAdminKey);
        if (authError != null) return authError;

        String newModel = request.get("model");
        if (newModel == null || newModel.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "errorCode", "VALIDATION_ERROR",
                    "message", "Campo 'model' é obrigatório"
            ));
        }

        // Valida se o modelo existe e está habilitado
        String provider = providerConfig.getDefaultProvider();
        if (!registry.isModelAvailable(provider, newModel)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "errorCode", "MODEL_NOT_AVAILABLE",
                    "message", "Modelo '" + newModel + "' não encontrado ou desabilitado no provider '" + provider + "'",
                    "availableModels", registry.getModelsForProvider(provider).stream()
                            .filter(m -> m.isEnabled())
                            .map(m -> m.getModelId())
                            .toList()
            ));
        }

        String oldModel = providerConfig.getDefaultModel();
        providerConfig.setDefaultModel(newModel);

        log.info("Modelo padrão alterado: {} -> {}", oldModel, newModel);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Modelo padrão alterado com sucesso",
                "previousModel", oldModel,
                "currentModel", newModel
        ));
    }

    /**
     * Verifica status da configuração (sem expor valores sensíveis).
     */
    @Operation(
            summary = "Status da configuracao",
            description = "Retorna status das configuracoes do Azure OpenAI (sem expor valores sensiveis)"
    )
    @SecurityRequirements({
            @SecurityRequirement(name = "apiKey"),
            @SecurityRequirement(name = "adminKey")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status retornado"),
            @ApiResponse(responseCode = "403", description = "Admin key invalida")
    })
    @GetMapping("/config-status")
    public ResponseEntity<Map<String, Object>> getConfigStatus(
            @RequestHeader(value = "X-Admin-Key", required = false) String requestAdminKey) {

        ResponseEntity<Map<String, Object>> authError = validateAdminKey(requestAdminKey);
        if (authError != null) return authError;

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("success", true);
        status.put("defaultProvider", providerConfig.getDefaultProvider());
        status.put("defaultModel", providerConfig.getDefaultModel());
        status.put("azureConfigured", azureConfig.isConfigured());
        status.put("azureEnabled", azureConfig.isEnabled());
        status.put("resourceName", azureConfig.getResourceName() != null ? azureConfig.getResourceName() : "não configurado");
        status.put("deploymentsCount", azureConfig.getDeployments() != null ? azureConfig.getDeployments().size() : 0);
        status.put("apiKeyConfigured", azureConfig.getApiKey() != null && !azureConfig.getApiKey().isBlank());
        status.put("availableModels", registry.getModelsForProvider(providerConfig.getDefaultProvider()).stream()
                .filter(m -> m.isEnabled())
                .map(m -> m.getModelId())
                .toList());

        return ResponseEntity.ok(status);
    }

    /**
     * Valida a admin key e retorna erro se inválida.
     */
    private ResponseEntity<Map<String, Object>> validateAdminKey(String requestAdminKey) {
        if (adminKey != null && !adminKey.isBlank()) {
            if (requestAdminKey == null || !requestAdminKey.equals(adminKey)) {
                log.warn("Tentativa de acesso admin com chave inválida");
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "errorCode", "FORBIDDEN",
                        "message", "Admin key inválida"
                ));
            }
        }
        return null;
    }
}
