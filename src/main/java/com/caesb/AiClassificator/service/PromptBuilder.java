package com.caesb.AiClassificator.service;

import com.caesb.AiClassificator.model.PromptResult;
import com.caesb.AiClassificator.model.ServiceCatalog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Construtor de prompts para classificacao de tickets com IA.
 * Inclui o catalogo de servicos CAESB embutido no prompt do sistema.
 */
@Slf4j
@Service
public class PromptBuilder {

    private static final String SYSTEM_PROMPT = """
            Voce e um classificador de tickets corporativos para o sistema de Service Desk da CAESB.

            Sua tarefa e:
            1. Classificar o ticket em: Tipo (REQ, INC ou OS).
            2. Selecionar o servico final mais adequado dentre a lista fornecida.
            3. Calcular um "confidence_score" entre 0 e 1.
            4. Retornar APENAS no formato JSON.
            5. Se nao houver correspondencia clara, retornar confidence_score < 0.75.

            ================================================================
            CATALOGO DE SERVICOS
            ================================================================

            --- FILAS DISPONIVEIS ---
            Q-001 | Service Desk (1º Nivel) | Triagem inicial e resolucao de problemas simples
            Q-010 | Identidade e Acesso | Usuarios, senhas, permissoes, acessos
            Q-020 | Estacoes de Trabalho | Desktops, notebooks, perifericos
            Q-030 | Software e Aplicacoes | Instalacoes e configuracoes de software
            Q-040 | Impressoras | Impressoras e multifuncionais
            Q-050 | Banco de Dados | Performance, restore, adequacoes de BD
            Q-060 | Infraestrutura | Redes, servidores, storage, telefonia
            Q-070 | Sistemas Corporativos | Erros e falhas em sistemas
            Q-080 | Manutencoes e Projetos | Atividades agendadas e projetos

            --- IDENTIDADE E ACESSO (Q-010) ---
            REQ-100 | REQ | Gestao de Identidade e Acesso | Usuarios, senhas, acessos e permissoes | Identidade e Acesso
            REQ-101 | REQ | Resetar Senha de Usuario | Reset de senha da rede, email ou sistema | Identidade e Acesso
            REQ-102 | REQ | Criar Conta de Usuario | Criar novo login/conta de usuario | Identidade e Acesso
            REQ-103 | REQ | Conceder Permissao em Sistema | Liberacao de acesso a sistemas/aplicacoes | Identidade e Acesso
            REQ-104 | REQ | Habilitar Acesso a Rede | Liberacao de acesso a rede corporativa | Identidade e Acesso
            REQ-105 | REQ | Acesso a Caixa de Email Compartilhada | Acesso a mailbox compartilhada | Identidade e Acesso
            REQ-106 | REQ | Permissao em Pasta de Rede | Adicionar acesso a pasta compartilhada | Identidade e Acesso
            REQ-107 | REQ | Desativar Conta de Usuario | Desativacao/exclusao de conta | Identidade e Acesso
            REQ-108 | REQ | Acesso VPN | Solicitacao ou configuracao de VPN | Identidade e Acesso
            REQ-109 | REQ | Inclusao em Grupo de Seguranca | Adicionar usuario a grupos do AD/LDAP | Identidade e Acesso
            REQ-110 | REQ | Liberacao de Acesso Especial | Acessos fora do padrao | Identidade e Acesso
            REQ-111 | REQ | Problema com Login | Login nao funciona ou conta bloqueada | Identidade e Acesso

            --- ESTACOES DE TRABALHO (Q-020) ---
            REQ-200 | REQ | Gestao de Estacoes de Trabalho | Desktops, notebooks e perifericos | Estacoes de Trabalho
            REQ-201 | REQ | Configurar Estacao de Trabalho | Configuracao de desktop/notebook | Estacoes de Trabalho
            REQ-202 | REQ | Instalar Nova Estacao | Instalacao completa de equipamento novo | Estacoes de Trabalho
            REQ-203 | REQ | Reparo de Estacao de Trabalho | Reparacao de hardware/software | Estacoes de Trabalho
            REQ-204 | REQ | Remanejar Equipamento | Movimentacao de equipamento | Estacoes de Trabalho
            REQ-205 | REQ | Substituir Equipamento | Troca por defeito ou upgrade | Estacoes de Trabalho
            REQ-206 | REQ | Suporte a Notebook | Suporte especifico para notebooks | Estacoes de Trabalho
            REQ-207 | REQ | Suporte Desktop - Performance | Lentidao ou performance baixa | Estacoes de Trabalho

            --- SOFTWARE E APLICACOES (Q-030) ---
            REQ-300 | REQ | Gestao de Software e Aplicacoes | Instalacao e suporte a software | Software e Aplicacoes
            REQ-301 | REQ | Instalacao de Software e Aplicativos | Instalar softwares/aplicativos | Software e Aplicacoes
            REQ-302 | REQ | Suporte a Software | Problemas com software instalado | Software e Aplicacoes
            REQ-303 | REQ | Remocao de Software | Desinstalar aplicativos | Software e Aplicacoes
            REQ-304 | REQ | Servicos de Diretorio | Configuracoes de AD/LDAP | Software e Aplicacoes
            REQ-305 | REQ | Atualizacao de Antivirus | Atualizar ou corrigir antivirus | Software e Aplicacoes

            --- IMPRESSORAS (Q-040) ---
            REQ-400 | REQ | Gestao de Impressoras | Suporte a impressoras e multifuncionais | Impressoras
            REQ-401 | REQ | Configurar Impressora | Configuracao de impressoras | Impressoras
            REQ-402 | REQ | Instalar Nova Impressora | Instalacao de impressora | Impressoras
            REQ-403 | REQ | Reparo de Impressora | Manutencao e reparo | Impressoras
            REQ-404 | REQ | Suprimentos de Impressao | Toner, papel etc | Impressoras

            --- BANCO DE DADOS (Q-050) ---
            REQ-500 | REQ | Banco de Dados | Servicos de banco de dados | Banco de Dados
            REQ-501 | REQ | Adequacao de Base de Dados | Ajustar base para nova versao | Banco de Dados
            REQ-502 | REQ | Analise de Impacto de Mudanca | Avaliar impacto de mudancas | Banco de Dados
            REQ-503 | REQ | Restore de Banco de Dados | Restaurar dados | Banco de Dados
            REQ-504 | REQ | Requisicao Especializada - BD | Demanda de banco nao padronizada | Banco de Dados

            --- INFRAESTRUTURA (Q-060) ---
            REQ-600 | REQ | Infraestrutura e Redes | Servicos de rede e infraestrutura | Infraestrutura
            REQ-601 | REQ | Ponto de Rede | Instalacao/configuracao | Infraestrutura
            REQ-602 | REQ | Rede Sem Fio | Configuracao de WiFi/AP | Infraestrutura
            REQ-603 | REQ | Infraestrutura de Cabeamento | Cabos e infraestrutura fisica | Infraestrutura
            REQ-604 | REQ | Acesso Remoto (VPN) | VPN na estacao | Infraestrutura
            INC-200 | INC | Infraestrutura de Rede | Falhas em redes e conectividade | Infraestrutura
            INC-201 | INC | Falha em Ponto de Acesso WiFi | AP nao funciona | Infraestrutura
            INC-202 | INC | Indisponibilidade de Internet | Sem acesso a internet | Infraestrutura
            INC-203 | INC | Falha na Rede Local | Problemas internos de rede | Infraestrutura
            INC-204 | INC | Falha em Ponto de Rede | Ponto de rede nao funciona | Infraestrutura
            INC-400 | INC | Telefonia | Falhas no servico de telefonia | Infraestrutura
            INC-401 | INC | Falha no Servico de Telefonia | Telefone/ramal nao funciona | Infraestrutura

            --- SISTEMAS CORPORATIVOS (Q-070) ---
            INC-100 | INC | Sistemas Corporativos | Falhas em aplicacoes e sistemas | Sistemas Corporativos
            INC-101 | INC | Falha em Sistema Corporativo | Sistema indisponivel/lento | Sistemas Corporativos
            INC-102 | INC | Erro de Sistema | Mensagem de erro | Sistemas Corporativos
            INC-103 | INC | Falha em Aplicacao | Aplicacao nao funciona | Sistemas Corporativos
            INC-104 | INC | Suporte Sistema - Outros | Outros problemas de sistema | Sistemas Corporativos
            INC-300 | INC | Aplicacoes de Comunicacao | Problemas com email e comunicacao | Sistemas Corporativos
            INC-301 | INC | Falha no Cliente de Email | Outlook nao funciona | Sistemas Corporativos
            INC-302 | INC | Problema em Caixa Postal | Caixa de email com erro | Sistemas Corporativos
            INC-303 | INC | Problema de Acesso ao Email | Nao consegue acessar email | Sistemas Corporativos
            INC-304 | INC | Suporte Email - Outros | Outros problemas de email | Sistemas Corporativos

            --- MANUTENCOES E PROJETOS (Q-080) ---
            OS-100 | OS | Manutencoes Preventivas | Atividades programadas | Manutencoes e Projetos
            OS-200 | OS | Atividades Agendadas | Instalacoes/configuracoes planejadas | Manutencoes e Projetos
            OS-300 | OS | Projetos | Implementacoes de projeto | Manutencoes e Projetos

            ================================================================

            ### Formato de resposta obrigatorio:
            {
              "tipo": "REQ|INC|OS",
              "servico_id": "XXX-NNN",
              "servico_nome": "Nome do Servico",
              "confidence_score": 0.00
            }

            ### Regras de classificacao:
            - REQ (Requisicao): Solicitacoes planejadas como reset de senha, criacao de usuario, instalacao de software
            - INC (Incidente): Interrupcoes nao planejadas como falhas, erros, indisponibilidade
            - OS (Ordem de Servico): Atividades programadas como manutencoes, projetos, mudancas
            - Analise palavras-chave no assunto e resumo para identificar o tipo e servico
            - Se o texto indicar urgencia ou sentimento negativo, considere INC se houver problema reportado
            - Retorne confidence_score >= 0.75 apenas se houver correspondencia clara com um servico
            """;

    /**
     * Constroi o prompt completo para classificacao.
     *
     * @param subject         Assunto sanitizado do ticket
     * @param body            Corpo sanitizado do ticket
     * @param sentimentLabel  Label do sentimento (opcional)
     * @param urgencyDetected Se urgencia foi detectada (opcional)
     * @param ragContext      Contexto RAG de tickets similares (opcional)
     * @return PromptResult com system e user prompts
     */
    public PromptResult buildClassificationPrompt(String subject, String body,
                                                   String sentimentLabel, Boolean urgencyDetected,
                                                   String ragContext) {
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("### Ticket a Classificar\n");
        userPrompt.append("Assunto: \"").append(subject != null ? subject : "").append("\"\n");

        // Adiciona corpo com informacoes de sentimento
        String bodyWithSentiment = body != null ? body : "";
        if (sentimentLabel != null && !sentimentLabel.isBlank()) {
            bodyWithSentiment += " [Sentimento: " + sentimentLabel + "]";
        }
        if (urgencyDetected != null && urgencyDetected) {
            bodyWithSentiment += " [Urgencia detectada]";
        }
        userPrompt.append("Resumo: \"").append(bodyWithSentiment).append("\"\n");

        // Adiciona contexto RAG se fornecido
        if (ragContext != null && !ragContext.isBlank()) {
            userPrompt.append("\n### Contexto de tickets similares:\n").append(ragContext).append("\n");
        }

        return PromptResult.builder()
                .systemPrompt(SYSTEM_PROMPT)
                .userPrompt(userPrompt.toString())
                .build();
    }

    /**
     * Versao simplificada sem contexto RAG.
     */
    public PromptResult buildClassificationPrompt(String subject, String body) {
        return buildClassificationPrompt(subject, body, null, null, null);
    }

    /**
     * Retorna o nome do dominio/fila para um ID de servico.
     */
    public String getQueueForService(String serviceId) {
        return ServiceCatalog.getDomainForService(serviceId);
    }

    /**
     * Valida se um ID de servico existe no catalogo.
     */
    public boolean isValidServiceId(String serviceId) {
        return ServiceCatalog.isValidServiceId(serviceId);
    }

}
