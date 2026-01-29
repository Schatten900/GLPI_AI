package com.caesb.AiClassificator.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuracao do OpenAPI/Swagger para documentacao da API.
 * Acessivel em /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AiClassificator API")
                        .version("1.0.0")
                        .description("""
                                Microsservico de classificacao automatica de tickets de Service Desk usando IA.
                                
                                **Integracao com GLPI:**
                                Este servico e chamado via plugin HTTP do ITSM GLPI para classificar 
                                tickets automaticamente em filas/servicos do catalogo CAESB.
                                
                                **Autenticacao:**
                                Todas as requisicoes (exceto /health e /catalog) requerem header X-API-Key.
                                Operacoes administrativas requerem tambem X-Admin-Key.
                                
                                **Modelos disponiveis:**
                                - gpt-4o-mini (rapido, economico)
                                - gpt-4o (alta qualidade)
                                - gpt-4 (robusto)
                                """)
                        .contact(new Contact()
                                .name("CAESB - TI")
                                .email("ti@caesb.df.gov.br"))
                        .license(new License()
                                .name("Proprietario")
                                .url("https://caesb.df.gov.br")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Servidor local"),
                        new Server()
                                .url("http://aiclassificator:8080")
                                .description("Container Docker")))
                .components(new Components()
                        .addSecuritySchemes("apiKey", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")
                                .description("API Key para autenticacao"))
                        .addSecuritySchemes("adminKey", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Admin-Key")
                                .description("Admin Key para operacoes administrativas")))
                .addSecurityItem(new SecurityRequirement().addList("apiKey"));
    }
}
