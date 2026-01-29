package com.caesb.AiClassificator.model;

import lombok.*;


/**
 * Representa uma fila/dominio no catalogo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Queue {
    private String id;
    private String name;
    private String description;
}
