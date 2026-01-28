# AiClassificator

Microsservico de classificacao de tickets com Inteligencia Artificial para o Service Desk da CAESB.

## Funcionalidades

- **Classificacao automatica de tickets** usando IA (OpenAI GPT-4o-mini)
- **Analise de sentimento** com lexico em portugues
- **Deteccao de urgencia** para priorizacao automatica
- **Sanitizacao de PII** (emails, CPF, CNPJ, telefones, IPs)
- **Catalogo de servicos CAESB** com 50+ servicos mapeados
- **Circuit breaker e retry** para resiliencia
- **API REST** para integracao com sistemas externos (GLPI, Znuny, etc)

## Arquitetura

```
┌─────────────────┐     ┌──────────────────────────────────────────────┐
│   GLPI/Znuny    │────▶│            AiClassificator                   │
│  (Ticket System)│     │  ┌─────────┐  ┌───────────┐  ┌───────────┐   │
└─────────────────┘     │  │Sanitizer│─▶│ Sentiment │─▶│  Prompt   │   │
                        │  └─────────┘  │ Analyzer  │  │  Builder  │   │
                        │               └───────────┘  └─────┬─────┘   │
                        │                                    │         │
                        │  ┌─────────────────────────────────▼──────┐  │
                        │  │           OpenAI Client                │  │
                        │  │    (Circuit Breaker + Retry)           │  │
                        │  └─────────────────────────────────┬──────┘  │
                        └────────────────────────────────────┼─────────┘
                                                             │
                                                    ┌────────▼────────┐
                                                    │   OpenAI API    │
                                                    │   (GPT-4o-mini) │
                                                    └─────────────────┘
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
- API Key da OpenAI

## Configuracao

### Variaveis de Ambiente

| Variavel | Descricao | Padrao |
|----------|-----------|--------|
| `OPENAI_API_KEY` | Chave de API da OpenAI | (obrigatorio) |
| `SERVER_PORT` | Porta do servidor | 8080 |

### application.yml

```yaml
ai:
  openai:
    api-key: ${OPENAI_API_KEY}
    model: gpt-4o-mini
    temperature: 0.3
    max-tokens: 500

  classification:
    confidence-threshold: 0.75
    fallback-queue: Service Desk (1º Nivel)

  sanitizer:
    body-max-length: 300
    sanitize-pii: true
```

## Executando

### Desenvolvimento

```bash
# Definir API Key
# Windows
set OPENAI_API_KEY=sk-sua-chave-aqui

# Linux/Mac
export OPENAI_API_KEY=sk-sua-chave-aqui

# Compilar e executar
./mvnw spring-boot:run
```

### Producao

```bash
# Compilar
./mvnw clean package -DskipTests

# Executar
java -jar target/AiClassificator-0.0.1-SNAPSHOT.jar
```

## API Reference

### Classificar Ticket

```http
POST /api/v1/classify
Content-Type: application/json
```

**Request:**
```json
{
  "subject": "Nao consigo acessar minha conta",
  "body": "Esqueci minha senha e nao consigo fazer login no sistema. Preciso urgente!",
  "senderEmail": "usuario@caesb.df.gov.br",
  "ticketId": "12345"
}
```

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
- **Timeout**: 30 segundos por requisicao
- **Fallback**: Encaminha para fila manual em caso de falha

## Estrutura do Projeto

```
src/main/java/com/caesb/AiClassificator/
├── AiClassificatorApplication.java
├── client/
│   ├── AIProviderClient.java      # Interface do provider
│   └── OpenAIClient.java          # Implementacao OpenAI
├── config/
│   ├── AIProviderConfig.java      # Configuracoes de IA
│   ├── ResilienceConfig.java      # Circuit breaker/retry
│   └── SecurityConfig.java        # Spring Security
├── controller/
│   └── ClassificationController.java
├── exception/
│   ├── ClassificationException.java
│   └── GlobalExceptionHandler.java
├── model/
│   ├── ClassificationRequest.java
│   ├── ClassificationResponse.java
│   └── ServiceCatalog.java        # Catalogo de 50+ servicos
└── service/
    ├── ClassificationService.java  # Orquestracao
    ├── PromptBuilder.java          # Construtor de prompts
    ├── Sanitizer.java              # Sanitizacao de PII
    └── SentimentAnalyzer.java      # Analise de sentimento
```

## Integracao

### GLPI

Para integrar com o GLPI, crie um plugin que intercepte o evento `TicketCreated` e chame a API:

```php
$response = file_get_contents('http://aiclassificator:8080/api/v1/classify', false,
    stream_context_create([
        'http' => [
            'method' => 'POST',
            'header' => 'Content-Type: application/json',
            'content' => json_encode([
                'subject' => $ticket->fields['name'],
                'body' => $ticket->fields['content'],
                'ticketId' => $ticket->getID()
            ])
        ]
    ])
);
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
