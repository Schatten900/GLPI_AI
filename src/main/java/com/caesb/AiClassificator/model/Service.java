package com.caesb.AiClassificator.model;
import lombok.*;


/**
 * Representa um servico no catalogo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Service {
    private String id;
    private String type;         // REQ, INC, OS
    private String name;
    private String description;
    private String domain;       // Fila/area responsavel
    private String queueId;
}
