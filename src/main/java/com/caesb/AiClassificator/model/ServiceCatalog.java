package com.caesb.AiClassificator.model;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Catalogo de servicos CAESB para classificacao de tickets.
 * Contem todos os servicos disponiveis organizados por tipo e dominio.
 */
@Data
public class ServiceCatalog {
    /**
     * Mapa de filas por ID.
     */
    private static final Map<String, Queue> QUEUES = new HashMap<>();

    /**
     * Mapa de servicos por ID.
     */
    private static final Map<String, Service> SERVICES = new HashMap<>();

    static {
        // Inicializa filas
        QUEUES.put("Q-001", Queue.builder().id("Q-001").name("Service Desk (1º Nivel)").description("Triagem inicial e resolucao de problemas simples").build());
        QUEUES.put("Q-010", Queue.builder().id("Q-010").name("Identidade e Acesso").description("Usuarios, senhas, permissoes, acessos").build());
        QUEUES.put("Q-020", Queue.builder().id("Q-020").name("Estacoes de Trabalho").description("Desktops, notebooks, perifericos").build());
        QUEUES.put("Q-030", Queue.builder().id("Q-030").name("Software e Aplicacoes").description("Instalacoes e configuracoes de software").build());
        QUEUES.put("Q-040", Queue.builder().id("Q-040").name("Impressoras").description("Impressoras e multifuncionais").build());
        QUEUES.put("Q-050", Queue.builder().id("Q-050").name("Banco de Dados").description("Performance, restore, adequacoes de BD").build());
        QUEUES.put("Q-060", Queue.builder().id("Q-060").name("Infraestrutura").description("Redes, servidores, storage, telefonia").build());
        QUEUES.put("Q-070", Queue.builder().id("Q-070").name("Sistemas Corporativos").description("Erros e falhas em sistemas").build());
        QUEUES.put("Q-080", Queue.builder().id("Q-080").name("Manutencoes e Projetos").description("Atividades agendadas e projetos").build());

        // =====================================================
        // IDENTIDADE E ACESSO (Q-010)
        // =====================================================
        SERVICES.put("REQ-100", Service.builder().id("REQ-100").type("REQ").name("Gestao de Identidade e Acesso").description("Usuarios, senhas, acessos e permissoes").domain("Identidade e Acesso").queueId("Q-010").build());
        SERVICES.put("REQ-101", Service.builder().id("REQ-101").type("REQ").name("Resetar Senha de Usuario").description("Reset de senha da rede, email ou sistema").domain("Identidade e Acesso").queueId("Q-010").build());
        SERVICES.put("REQ-102", Service.builder().id("REQ-102").type("REQ").name("Criar Conta de Usuario").description("Criar novo login/conta de usuario").domain("Identidade e Acesso").queueId("Q-010").build());
        SERVICES.put("REQ-103", Service.builder().id("REQ-103").type("REQ").name("Conceder Permissao em Sistema").description("Liberacao de acesso a sistemas/aplicacoes").domain("Identidade e Acesso").queueId("Q-010").build());
        SERVICES.put("REQ-104", Service.builder().id("REQ-104").type("REQ").name("Habilitar Acesso a Rede").description("Liberacao de acesso a rede corporativa").domain("Identidade e Acesso").queueId("Q-010").build());
        SERVICES.put("REQ-105", Service.builder().id("REQ-105").type("REQ").name("Acesso a Caixa de Email Compartilhada").description("Acesso a mailbox compartilhada").domain("Identidade e Acesso").queueId("Q-010").build());
        SERVICES.put("REQ-106", Service.builder().id("REQ-106").type("REQ").name("Permissao em Pasta de Rede").description("Adicionar acesso a pasta compartilhada").domain("Identidade e Acesso").queueId("Q-010").build());
        SERVICES.put("REQ-107", Service.builder().id("REQ-107").type("REQ").name("Desativar Conta de Usuario").description("Desativacao/exclusao de conta").domain("Identidade e Acesso").queueId("Q-010").build());
        SERVICES.put("REQ-108", Service.builder().id("REQ-108").type("REQ").name("Acesso VPN").description("Solicitacao ou configuracao de VPN").domain("Identidade e Acesso").queueId("Q-010").build());
        SERVICES.put("REQ-109", Service.builder().id("REQ-109").type("REQ").name("Inclusao em Grupo de Seguranca").description("Adicionar usuario a grupos do AD/LDAP").domain("Identidade e Acesso").queueId("Q-010").build());
        SERVICES.put("REQ-110", Service.builder().id("REQ-110").type("REQ").name("Liberacao de Acesso Especial").description("Acessos fora do padrao").domain("Identidade e Acesso").queueId("Q-010").build());
        SERVICES.put("REQ-111", Service.builder().id("REQ-111").type("REQ").name("Problema com Login").description("Login nao funciona ou conta bloqueada").domain("Identidade e Acesso").queueId("Q-010").build());

        // =====================================================
        // ESTACOES DE TRABALHO (Q-020)
        // =====================================================
        SERVICES.put("REQ-200", Service.builder().id("REQ-200").type("REQ").name("Gestao de Estacoes de Trabalho").description("Desktops, notebooks e perifericos").domain("Estacoes de Trabalho").queueId("Q-020").build());
        SERVICES.put("REQ-201", Service.builder().id("REQ-201").type("REQ").name("Configurar Estacao de Trabalho").description("Configuracao de desktop/notebook").domain("Estacoes de Trabalho").queueId("Q-020").build());
        SERVICES.put("REQ-202", Service.builder().id("REQ-202").type("REQ").name("Instalar Nova Estacao").description("Instalacao completa de equipamento novo").domain("Estacoes de Trabalho").queueId("Q-020").build());
        SERVICES.put("REQ-203", Service.builder().id("REQ-203").type("REQ").name("Reparo de Estacao de Trabalho").description("Reparacao de hardware/software").domain("Estacoes de Trabalho").queueId("Q-020").build());
        SERVICES.put("REQ-204", Service.builder().id("REQ-204").type("REQ").name("Remanejar Equipamento").description("Movimentacao de equipamento").domain("Estacoes de Trabalho").queueId("Q-020").build());
        SERVICES.put("REQ-205", Service.builder().id("REQ-205").type("REQ").name("Substituir Equipamento").description("Troca por defeito ou upgrade").domain("Estacoes de Trabalho").queueId("Q-020").build());
        SERVICES.put("REQ-206", Service.builder().id("REQ-206").type("REQ").name("Suporte a Notebook").description("Suporte especifico para notebooks").domain("Estacoes de Trabalho").queueId("Q-020").build());
        SERVICES.put("REQ-207", Service.builder().id("REQ-207").type("REQ").name("Suporte Desktop - Performance").description("Lentidao ou performance baixa").domain("Estacoes de Trabalho").queueId("Q-020").build());

        // =====================================================
        // SOFTWARE E APLICACOES (Q-030)
        // =====================================================
        SERVICES.put("REQ-300", Service.builder().id("REQ-300").type("REQ").name("Gestao de Software e Aplicacoes").description("Instalacao e suporte a software").domain("Software e Aplicacoes").queueId("Q-030").build());
        SERVICES.put("REQ-301", Service.builder().id("REQ-301").type("REQ").name("Instalacao de Software e Aplicativos").description("Instalar softwares/aplicativos").domain("Software e Aplicacoes").queueId("Q-030").build());
        SERVICES.put("REQ-302", Service.builder().id("REQ-302").type("REQ").name("Suporte a Software").description("Problemas com software instalado").domain("Software e Aplicacoes").queueId("Q-030").build());
        SERVICES.put("REQ-303", Service.builder().id("REQ-303").type("REQ").name("Remocao de Software").description("Desinstalar aplicativos").domain("Software e Aplicacoes").queueId("Q-030").build());
        SERVICES.put("REQ-304", Service.builder().id("REQ-304").type("REQ").name("Servicos de Diretorio").description("Configuracoes de AD/LDAP").domain("Software e Aplicacoes").queueId("Q-030").build());
        SERVICES.put("REQ-305", Service.builder().id("REQ-305").type("REQ").name("Atualizacao de Antivirus").description("Atualizar ou corrigir antivirus").domain("Software e Aplicacoes").queueId("Q-030").build());

        // =====================================================
        // IMPRESSORAS (Q-040)
        // =====================================================
        SERVICES.put("REQ-400", Service.builder().id("REQ-400").type("REQ").name("Gestao de Impressoras").description("Suporte a impressoras e multifuncionais").domain("Impressoras").queueId("Q-040").build());
        SERVICES.put("REQ-401", Service.builder().id("REQ-401").type("REQ").name("Configurar Impressora").description("Configuracao de impressoras").domain("Impressoras").queueId("Q-040").build());
        SERVICES.put("REQ-402", Service.builder().id("REQ-402").type("REQ").name("Instalar Nova Impressora").description("Instalacao de impressora").domain("Impressoras").queueId("Q-040").build());
        SERVICES.put("REQ-403", Service.builder().id("REQ-403").type("REQ").name("Reparo de Impressora").description("Manutencao e reparo").domain("Impressoras").queueId("Q-040").build());
        SERVICES.put("REQ-404", Service.builder().id("REQ-404").type("REQ").name("Suprimentos de Impressao").description("Toner, papel etc").domain("Impressoras").queueId("Q-040").build());

        // =====================================================
        // BANCO DE DADOS (Q-050)
        // =====================================================
        SERVICES.put("REQ-500", Service.builder().id("REQ-500").type("REQ").name("Banco de Dados").description("Servicos de banco de dados").domain("Banco de Dados").queueId("Q-050").build());
        SERVICES.put("REQ-501", Service.builder().id("REQ-501").type("REQ").name("Adequacao de Base de Dados").description("Ajustar base para nova versao").domain("Banco de Dados").queueId("Q-050").build());
        SERVICES.put("REQ-502", Service.builder().id("REQ-502").type("REQ").name("Analise de Impacto de Mudanca").description("Avaliar impacto de mudancas").domain("Banco de Dados").queueId("Q-050").build());
        SERVICES.put("REQ-503", Service.builder().id("REQ-503").type("REQ").name("Restore de Banco de Dados").description("Restaurar dados").domain("Banco de Dados").queueId("Q-050").build());
        SERVICES.put("REQ-504", Service.builder().id("REQ-504").type("REQ").name("Requisicao Especializada - BD").description("Demanda de banco nao padronizada").domain("Banco de Dados").queueId("Q-050").build());

        // =====================================================
        // INFRAESTRUTURA (Q-060)
        // =====================================================
        SERVICES.put("REQ-600", Service.builder().id("REQ-600").type("REQ").name("Infraestrutura e Redes").description("Servicos de rede e infraestrutura").domain("Infraestrutura").queueId("Q-060").build());
        SERVICES.put("REQ-601", Service.builder().id("REQ-601").type("REQ").name("Ponto de Rede").description("Instalacao/configuracao").domain("Infraestrutura").queueId("Q-060").build());
        SERVICES.put("REQ-602", Service.builder().id("REQ-602").type("REQ").name("Rede Sem Fio").description("Configuracao de WiFi/AP").domain("Infraestrutura").queueId("Q-060").build());
        SERVICES.put("REQ-603", Service.builder().id("REQ-603").type("REQ").name("Infraestrutura de Cabeamento").description("Cabos e infraestrutura fisica").domain("Infraestrutura").queueId("Q-060").build());
        SERVICES.put("REQ-604", Service.builder().id("REQ-604").type("REQ").name("Acesso Remoto (VPN)").description("VPN na estacao").domain("Infraestrutura").queueId("Q-060").build());
        SERVICES.put("INC-200", Service.builder().id("INC-200").type("INC").name("Infraestrutura de Rede").description("Falhas em redes e conectividade").domain("Infraestrutura").queueId("Q-060").build());
        SERVICES.put("INC-201", Service.builder().id("INC-201").type("INC").name("Falha em Ponto de Acesso WiFi").description("AP nao funciona").domain("Infraestrutura").queueId("Q-060").build());
        SERVICES.put("INC-202", Service.builder().id("INC-202").type("INC").name("Indisponibilidade de Internet").description("Sem acesso a internet").domain("Infraestrutura").queueId("Q-060").build());
        SERVICES.put("INC-203", Service.builder().id("INC-203").type("INC").name("Falha na Rede Local").description("Problemas internos de rede").domain("Infraestrutura").queueId("Q-060").build());
        SERVICES.put("INC-204", Service.builder().id("INC-204").type("INC").name("Falha em Ponto de Rede").description("Ponto de rede nao funciona").domain("Infraestrutura").queueId("Q-060").build());
        SERVICES.put("INC-400", Service.builder().id("INC-400").type("INC").name("Telefonia").description("Falhas no servico de telefonia").domain("Infraestrutura").queueId("Q-060").build());
        SERVICES.put("INC-401", Service.builder().id("INC-401").type("INC").name("Falha no Servico de Telefonia").description("Telefone/ramal nao funciona").domain("Infraestrutura").queueId("Q-060").build());

        // =====================================================
        // SISTEMAS CORPORATIVOS (Q-070)
        // =====================================================
        SERVICES.put("INC-100", Service.builder().id("INC-100").type("INC").name("Sistemas Corporativos").description("Falhas em aplicacoes e sistemas").domain("Sistemas Corporativos").queueId("Q-070").build());
        SERVICES.put("INC-101", Service.builder().id("INC-101").type("INC").name("Falha em Sistema Corporativo").description("Sistema indisponivel/lento").domain("Sistemas Corporativos").queueId("Q-070").build());
        SERVICES.put("INC-102", Service.builder().id("INC-102").type("INC").name("Erro de Sistema").description("Mensagem de erro").domain("Sistemas Corporativos").queueId("Q-070").build());
        SERVICES.put("INC-103", Service.builder().id("INC-103").type("INC").name("Falha em Aplicacao").description("Aplicacao nao funciona").domain("Sistemas Corporativos").queueId("Q-070").build());
        SERVICES.put("INC-104", Service.builder().id("INC-104").type("INC").name("Suporte Sistema - Outros").description("Outros problemas de sistema").domain("Sistemas Corporativos").queueId("Q-070").build());
        SERVICES.put("INC-300", Service.builder().id("INC-300").type("INC").name("Aplicacoes de Comunicacao").description("Problemas com email e comunicacao").domain("Sistemas Corporativos").queueId("Q-070").build());
        SERVICES.put("INC-301", Service.builder().id("INC-301").type("INC").name("Falha no Cliente de Email").description("Outlook nao funciona").domain("Sistemas Corporativos").queueId("Q-070").build());
        SERVICES.put("INC-302", Service.builder().id("INC-302").type("INC").name("Problema em Caixa Postal").description("Caixa de email com erro").domain("Sistemas Corporativos").queueId("Q-070").build());
        SERVICES.put("INC-303", Service.builder().id("INC-303").type("INC").name("Problema de Acesso ao Email").description("Nao consegue acessar email").domain("Sistemas Corporativos").queueId("Q-070").build());
        SERVICES.put("INC-304", Service.builder().id("INC-304").type("INC").name("Suporte Email - Outros").description("Outros problemas de email").domain("Sistemas Corporativos").queueId("Q-070").build());

        // =====================================================
        // MANUTENCOES E PROJETOS (Q-080)
        // =====================================================
        SERVICES.put("OS-100", Service.builder().id("OS-100").type("OS").name("Manutencoes Preventivas").description("Atividades programadas").domain("Manutencoes e Projetos").queueId("Q-080").build());
        SERVICES.put("OS-200", Service.builder().id("OS-200").type("OS").name("Atividades Agendadas").description("Instalacoes/configuracoes planejadas").domain("Manutencoes e Projetos").queueId("Q-080").build());
        SERVICES.put("OS-300", Service.builder().id("OS-300").type("OS").name("Projetos").description("Implementacoes de projeto").domain("Manutencoes e Projetos").queueId("Q-080").build());
    }

    /**
     * Retorna um servico pelo ID.
     */
    public static Service getService(String serviceId) {
        return SERVICES.get(serviceId);
    }

    /**
     * Retorna uma fila pelo ID.
     */
    public static Queue getQueue(String queueId) {
        return QUEUES.get(queueId);
    }

    /**
     * Retorna a fila associada a um servico.
     */
    public static Queue getQueueForService(String serviceId) {
        Service service = SERVICES.get(serviceId);
        if (service != null) {
            return QUEUES.get(service.getQueueId());
        }
        return null;
    }

    /**
     * Verifica se um ID de servico e valido.
     */
    public static boolean isValidServiceId(String serviceId) {
        return SERVICES.containsKey(serviceId);
    }

    /**
     * Retorna todos os servicos.
     */
    public static Map<String, Service> getAllServices() {
        return new HashMap<>(SERVICES);
    }

    /**
     * Retorna todas as filas.
     */
    public static Map<String, Queue> getAllQueues() {
        return new HashMap<>(QUEUES);
    }

    /**
     * Retorna o nome do dominio/fila para um servico.
     */
    public static String getDomainForService(String serviceId) {
        Service service = SERVICES.get(serviceId);
        return service != null ? service.getDomain() : null;
    }
}
