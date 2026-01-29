# AiClassificator

Microsservico de classificacao de tickets com Inteligencia Artificial para o Service Desk da CAESB.

## Funcionalidades

- **Classificacao automatica de tickets** usando IA (Azure OpenAI)
- **Suporte a multiplos modelos** (GPT-4o, GPT-4o-mini, GPT-4, Claude via Azure AI Foundry)
- **Selecao dinamica de provider/modelo** via API (para integracao com GLPI)
- **Analise de sentimento** com lexico em portugues
- **Deteccao de urgencia** para priorizacao automatica
- **Sanitizacao de PII** (emails, CPF, CNPJ, telefones, IPs)
- **Catalogo de servicos CAESB** com 50+ servicos mapeados
- **Circuit breaker e retry** para resiliencia
- **Fallback automatico** entre modelos em caso de falha
- **Cache de idempotencia** para evitar reprocessamento
- **Autenticacao via API Key** para seguranca
- **Documentacao OpenAPI/Swagger** interativa
- **API REST** para integracao com sistemas externos (GLPI, Znuny, etc)

## Arquitetura

```
┌─────────────────┐        ┌──────────────────────────────────────────────┐
│      GLPI       │───────▶│            AiClassificator                   │
│  X-API-Key: xxx │        │  ┌─────────┐  ┌───────────┐  ┌───────────┐   │
│  provider: xxx  │        │  │Sanitizer│─▶│ Sentiment │─▶│  Prompt   │   │
│  model: xxx     │        │  └─────────┘  │ Analyzer  │  │  Builder  │   │
└─────────────────┘        │               └───────────┘  └─────┬─────┘   │
                           │                                    │         │
                           │  ┌─────────────────────────────────▼──────┐  │
                           │  │         AIProviderFactory              │  │
                           │  │  ┌──────────────────────────────────┐  │  │
                           │  │  │      AzureOpenAIClient           │  │  │
                           │  │  │  (gpt-4o-mini ↔ gpt-4o fallback) │  │  │
                           │  │  └──────────────────────────────────┘  │  │
                           │  └────────────────────────────────────────┘  │
                           └──────────────────────────────────────────────┘
                                              │
                                    ┌─────────▼─────────┐
                                    │  Azure OpenAI     │
                                    │  (GPT, Claude,    │
                                    │   Gemini...)      │
                                    └───────────────────┘
```

## Tecnologias

- Java 17
- Spring Boot 3.5
- Spring Security
- Resilience4j (Circuit Breaker, Retry)
- Lombok
- Maven

## Pre-requisitos

- Java 17+
- Maven 3.8+
- Conta Azure OpenAI com pelo menos um deployment criado

## Configuracao

### Variaveis de Ambiente

| Variavel | Descricao | Obrigatorio |
|----------|-----------|-------------|
| `API_KEY` | Chave para autenticacao da API (header X-API-Key) | Sim (producao) |
| `ADMIN_KEY` | Chave para operacoes administrativas | Sim (producao) |
| `AZURE_OPENAI_RESOURCE` | Nome do recurso Azure OpenAI | Sim |
| `AZURE_OPENAI_API_KEY` | Chave de API do Azure OpenAI | Sim |
| `SERVER_PORT` | Porta do servidor | Nao (default: 8080) |

> **Nota:** Em modo desenvolvimento (sem `API_KEY` configurada), todos os endpoints ficam abertos.

### application.yml

```yaml
# Seguranca
security:
  api-key: ${API_KEY:}      # Deixe vazio para modo dev
  admin-key: ${ADMIN_KEY:}

ai:
  # Provider e modelo padrao quando nao especificado na requisicao
  default-provider: azure-openai
  default-model: gpt-4o-mini

  # Azure OpenAI (provider principal)
  azure-openai:
    enabled: true
    resource-name: ${AZURE_OPENAI_RESOURCE:caesb-openai}
    api-key: ${AZURE_OPENAI_API_KEY:}
    api-version: "2024-02-01"
    deployments:
      gpt-4o-mini:
        deployment-name: gpt-4o-mini-deploy
        display-name: "GPT-4o Mini"
        description: "Modelo rapido e economico"
        enabled: true
      gpt-4o:
        deployment-name: gpt-4o-deploy
        display-name: "GPT-4o"
        description: "Modelo de alta qualidade"
        enabled: true
      claude-3-sonnet:
        deployment-name: claude-3-sonnet-deploy
        display-name: "Claude 3 Sonnet"
        description: "Claude 3 via Azure AI Foundry"
        enabled: false

  # Classificacao
  classification:
    confidence-threshold: 0.75
    fallback-queue: Service Desk (1º Nivel)

  # Cache de idempotencia
  cache:
    ttl-minutes: 5
    max-size: 1000

  # Sanitizacao
  sanitizer:
    body-max-length: 300
    sanitize-pii: true
```

## Executando

### Desenvolvimento

```bash
# Definir variaveis Azure OpenAI
# Windows
set AZURE_OPENAI_RESOURCE=seu-recurso-azure
set AZURE_OPENAI_API_KEY=sua-chave-azure

# Linux/Mac
export AZURE_OPENAI_RESOURCE=seu-recurso-azure
export AZURE_OPENAI_API_KEY=sua-chave-azure

# Compilar e executar (modo dev - sem autenticacao)
./mvnw spring-boot:run
```

### Producao (Docker)

```bash
# Criar arquivo .env
cat > .env << EOF
API_KEY=sua-chave-api-secreta
ADMIN_KEY=sua-chave-admin-secreta
AZURE_OPENAI_RESOURCE=seu-recurso-azure
AZURE_OPENAI_API_KEY=sua-chave-azure
EOF

# Executar com docker-compose
docker-compose up -d
```

### Producao (JAR)

```bash
# Compilar
./mvnw clean package -DskipTests

# Executar com variaveis de ambiente
API_KEY=xxx ADMIN_KEY=yyy AZURE_OPENAI_API_KEY=zzz \
  java -jar target/AiClassificator-0.0.1-SNAPSHOT.jar
```

## Autenticacao

A API usa autenticacao via headers HTTP:

| Header | Descricao | Endpoints |
|--------|-----------|-----------|
| `X-API-Key` | Chave principal de acesso | Todos exceto publicos |
| `X-Admin-Key` | Chave para operacoes admin | `/api/v1/admin/*` |

### Endpoints Publicos (sem autenticacao)

- `GET /api/v1/health` - Health check
- `GET /api/v1/catalog/*` - Catalogo de servicos
- `GET /actuator/health` - Health do Actuator
- `GET /swagger-ui.html` - Documentacao Swagger

### Exemplo de Requisicao Autenticada

```bash
curl -X POST http://localhost:8080/api/v1/classify \
  -H "Content-Type: application/json" \
  -H "X-API-Key: sua-chave-api" \
  -d '{
    "subject": "Problema com senha",
    "body": "Nao consigo acessar minha conta",
    "ticketId": "12345"
  }'
```

## Documentacao da API (Swagger)

Acesse a documentacao interativa em:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

## API Reference

### Listar Providers e Modelos Disponiveis

```http
GET /api/v1/providers
X-API-Key: sua-chave-api
```

**Response:**
```json
{
  "providers": [
    {
      "id": "azure-openai",
      "displayName": "Azure OpenAI",
      "enabled": true,
      "models": [
        {
          "id": "gpt-4o-mini",
          "displayName": "GPT-4o Mini",
          "description": "Modelo rapido e economico",
          "enabled": true
        },
        {
          "id": "gpt-4o",
          "displayName": "GPT-4o",
          "description": "Modelo de alta qualidade",
          "enabled": true
        }
      ]
    }
  ],
  "defaultProvider": "azure-openai",
  "defaultModel": "gpt-4o-mini",
  "totalModels": 2
}
```

### Testar Conectividade com Provider/Modelo

```http
POST /api/v1/providers/test
Content-Type: application/json
X-API-Key: sua-chave-api
```

**Request:**
```json
{
  "provider": "azure-openai",
  "model": "gpt-4o-mini"
}
```

**Response:**
```json
{
  "success": true,
  "provider": "azure-openai",
  "model": "gpt-4o-mini",
  "latencyMs": 450,
  "message": "Conexao estabelecida com sucesso"
}
```

### Classificar Ticket

```http
POST /api/v1/classify
Content-Type: application/json
X-API-Key: sua-chave-api
```

**Request:**
```json
{
  "subject": "Nao consigo acessar minha conta",
  "body": "Esqueci minha senha e nao consigo fazer login no sistema. Preciso urgente!",
  "senderEmail": "usuario@caesb.df.gov.br",
  "ticketId": "12345",
  "provider": "azure-openai",
  "model": "gpt-4o-mini"
}
```

> **Nota:** Os campos `provider` e `model` sao opcionais. Se nao fornecidos, serao usados os valores default configurados.

**Response:**
```json
{
  "success": true,
  "status": "applied",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "REQ",
  "serviceId": "REQ-101",
  "serviceName": "Resetar Senha de Usuario",
  "queue": "Identidade e Acesso",
  "confidenceScore": 0.92,
  "thresholdMet": true,
  "sentimentLabel": "negative",
  "urgencyDetected": true,
  "criticalityScore": 3,
  "shouldIncreaseSeverity": true,
  "processingTimeMs": 1250,
  "message": "Classificacao aplicada automaticamente"
}
```

### Listar Servicos

```http
GET /api/v1/catalog/services
```

### Listar Filas

```http
GET /api/v1/catalog/queues
```

### Health Check

```http
GET /api/v1/health
```

## Catalogo de Servicos

O sistema suporta os seguintes tipos de tickets:

| Tipo | Descricao |
|------|-----------|
| **REQ** | Requisicao - Solicitacoes planejadas |
| **INC** | Incidente - Interrupcoes nao planejadas |
| **OS** | Ordem de Servico - Atividades programadas |

### Filas Disponiveis

| ID | Nome | Descricao |
|----|------|-----------|
| Q-010 | Identidade e Acesso | Usuarios, senhas, permissoes |
| Q-020 | Estacoes de Trabalho | Desktops, notebooks |
| Q-030 | Software e Aplicacoes | Instalacoes de software |
| Q-040 | Impressoras | Suporte a impressoras |
| Q-050 | Banco de Dados | Servicos de BD |
| Q-060 | Infraestrutura | Redes, servidores |
| Q-070 | Sistemas Corporativos | Erros em sistemas |
| Q-080 | Manutencoes e Projetos | Atividades agendadas |

## Resiliencia

O sistema implementa os seguintes padroes de resiliencia:

- **Circuit Breaker**: Abre apos 5 falhas em 10 chamadas (50%)
- **Retry**: 3 tentativas com backoff exponencial
- **Timeout**: 30 segundos por requisicao (connect: 5s, read: 30s)
- **Fallback de modelo**: Se gpt-4o falhar, tenta gpt-4o-mini automaticamente
- **Fallback manual**: Se IA indisponivel, encaminha para fila manual
- **Cache de idempotencia**: Evita reprocessar mesmo ticket em 5 minutos

### Fluxo de Fallback

```
Requisicao → Modelo solicitado
              ↓ (falha)
            Modelo fallback (gpt-4o-mini)
              ↓ (falha)
            Classificacao manual (fallback-queue)
```

## Estrutura do Projeto

```
src/main/java/com/caesb/AiClassificator/
├── AiClassificatorApplication.java
├── client/
│   ├── AIProviderClient.java      # Interface do provider
│   ├── AIProviderFactory.java     # Factory + fallback logic
│   ├── AIProviderRegistry.java    # Registro de providers/modelos
│   └── AzureOpenAIClient.java     # Cliente Azure OpenAI
├── config/
│   ├── AIProviderConfig.java      # Configuracoes gerais + cache
│   ├── ApiKeyAuthFilter.java      # Filtro de autenticacao
│   ├── AzureHealthIndicator.java  # Health check Azure
│   ├── AzureOpenAIConfig.java     # Configuracoes Azure
│   ├── OpenApiConfig.java         # Swagger/OpenAPI
│   ├── ResilienceConfig.java      # Circuit breaker/retry
│   ├── RestTemplateConfig.java    # HTTP client com timeouts
│   └── SecurityConfig.java        # Spring Security
├── controller/
│   ├── ClassificationController.java
│   ├── ConfigController.java      # Endpoints admin
│   └── ProviderController.java    # Endpoints de providers
├── exception/
│   ├── ClassificationException.java
│   └── GlobalExceptionHandler.java
├── model/
│   ├── AIDeployment.java          # Modelo de deployment
│   ├── AIRequest.java
│   ├── AIResponse.java
│   ├── ClassificationRequest.java
│   ├── ClassificationResponse.java
│   ├── ProviderInfo.java          # DTO lista providers
│   ├── ProvidersListResponse.java # Response lista providers
│   ├── ServiceCatalog.java        # Catalogo de 50+ servicos
│   ├── TestConnectionRequest.java # DTO teste conexao
│   └── TestConnectionResponse.java
└── service/
    ├── ClassificationCache.java   # Cache de idempotencia
    ├── ClassificationService.java # Orquestracao
    ├── PromptBuilder.java         # Construtor de prompts
    ├── Sanitizer.java             # Sanitizacao de PII
    └── SentimentAnalyzer.java     # Analise de sentimento
```

## Integracao

### GLPI

Para integrar com o GLPI, crie um plugin que intercepte o evento `TicketCreated` e chame a API:

```php
$apiKey = 'sua-chave-api';
$response = file_get_contents('http://aiclassificator:8080/api/v1/classify', false,
    stream_context_create([
        'http' => [
            'method' => 'POST',
            'header' => [
                'Content-Type: application/json',
                'X-API-Key: ' . $apiKey
            ],
            'content' => json_encode([
                'subject' => $ticket->fields['name'],
                'body' => $ticket->fields['content'],
                'ticketId' => $ticket->getID(),
                'model' => 'gpt-4o-mini'  // opcional
            ])
        ]
    ])
);

$result = json_decode($response, true);
if ($result['success'] && $result['status'] === 'applied') {
    // Atualiza o ticket com a classificacao
    $ticket->update([
        'id' => $ticket->getID(),
        'itilcategories_id' => $result['serviceId'],
        // ...
    ]);
}
```

### Znuny/OTRS

O sistema foi originalmente desenvolvido para Znuny. A integracao pode ser feita via GenericInterface WebService.

## Licenca

Copyright (C) 2024 CAESB - Companhia de Saneamento Ambiental do Distrito Federal

Este software e disponibilizado sob a licenca GPL v3. Veja o arquivo LICENSE para mais detalhes.

## Contribuicao

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/nova-feature`)
3. Commit suas mudancas (`git commit -m 'Adiciona nova feature'`)
4. Push para a branch (`git push origin feature/nova-feature`)
5. Abra um Pull Request
