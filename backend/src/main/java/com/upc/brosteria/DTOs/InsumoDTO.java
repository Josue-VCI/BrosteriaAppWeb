package com.upc.brosteria.DTOs;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class InsumoDTO {
    private Long id;
    private String name;
    private Double quantity;
    private String unit;
    private Double minimumStock;
    private LocalDateTime updatedAt;
}
