# Refatoracao Znuny AI Classifier -> Java Spring Boot

## Status Geral: Etapas 1-4 Concluidas

---

## Etapa 1 - Setup Projeto Java (Concluida)

1. [x] Criar projeto Spring Boot 3.x com Maven
2. [x] Adicionar dependencias (Resilience4j, Jackson, Spring Web/Security)
3. [x] Configurar application.yml com providers AI

---

## Etapa 2 - Core Services (Concluida)

4. [x] Implementar PromptBuilder com catalogo CAESB (50+ servicos)
5. [x] Implementar Sanitizer (mascarar email, CPF, telefone, PII)
6. [x] Implementar SentimentAnalyzer (lexico portugues)

---

## Etapa 3 - AI Client (Concluida)

7. [x] Implementar AIProviderClient com suporte a OpenAI
8. [x] Adicionar circuit breaker e retry com Resilience4j

---

## Etapa 4 - API REST (Concluida)

9. [x] Criar ClassificationController com endpoint POST /api/v1/classify
10. [x] Validacao de request e tratamento de erros

---

## Etapa 5 - Deploy (Pendente)

11. [ ] Criar Dockerfile otimizado
12. [ ] Criar docker-compose.yml
13. [ ] Documentar variaveis de ambiente

---

## Etapa 6 - Plugin GLPI (Pendente)

14. [ ] Criar plugin PHP basico para GLPI
15. [ ] Interceptar evento TicketCreated
16. [ ] Chamar microsservico e atualizar ticket

---

## Arquivos Implementados

### Models
- `ClassificationRequest.java` - Request com subject, body, sender
- `ClassificationResponse.java` - Response com tipo, servico, confianca
- `ServiceCatalog.java` - Catalogo de 50+ servicos CAESB

### Services
- `Sanitizer.java` - Sanitizacao de PII (email, CPF, telefone, IP)
- `SentimentAnalyzer.java` - Analise de sentimento lexico PT-BR
- `PromptBuilder.java` - Construtor de prompts com catalogo embutido
- `ClassificationService.java` - Orquestracao do pipeline completo

### Clients
- `AIProviderClient.java` - Interface para providers de IA
- `OpenAIClient.java` - Implementacao OpenAI com circuit breaker

### Controller
- `ClassificationController.java` - API REST /api/v1/classify

### Config
- `SecurityConfig.java` - Configuracao Spring Security
- `AIProviderConfig.java` - Configuracao de providers
- `ResilienceConfig.java` - Circuit breaker e retry
- `application.yml` - Configuracoes da aplicacao

### Exception
- `ClassificationException.java` - Excecao customizada
- `GlobalExceptionHandler.java` - Handler global de erros

---

## Como Executar

1. Definir variavel de ambiente:
   ```
   export OPENAI_API_KEY=sk-your-key-here
   ```

2. Compilar:
   ```
   mvn clean package
   ```

3. Executar:
   ```
   java -jar target/AiClassificator-0.0.1-SNAPSHOT.jar
   ```

4. Testar:
   ```
   curl -X POST http://localhost:8080/api/v1/classify \
     -H "Content-Type: application/json" \
     -d '{"subject":"Nao consigo acessar minha conta","body":"Esqueci minha senha e nao consigo fazer login no sistema"}'
   ```

---

## Endpoints Disponiveis

| Metodo | Endpoint | Descricao |
|--------|----------|-----------|
| POST | /api/v1/classify | Classifica um ticket |
| GET | /api/v1/catalog/services | Lista todos os servicos |
| GET | /api/v1/catalog/queues | Lista todas as filas |
| GET | /api/v1/catalog/services/{id} | Busca servico por ID |
| GET | /api/v1/health | Health check |
| GET | /actuator/health | Actuator health |
