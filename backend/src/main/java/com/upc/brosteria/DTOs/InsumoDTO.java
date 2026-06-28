package com.upc.brosteria.DTOs;

import lombok.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class InsumoDTO {
    private Long id;
    @NotBlank(message = "El nombre del insumo es requerido")
    @Size(max = 120, message = "El nombre del insumo es demasiado largo")
    private String name;
    @NotNull(message = "La cantidad es requerida")
    @PositiveOrZero(message = "La cantidad no puede ser negativa")
    private BigDecimal quantity;
    @NotBlank(message = "La unidad es requerida")
    @Size(max = 30, message = "La unidad es demasiado larga")
    private String unit;
    @NotNull(message = "El stock minimo es requerido")
    @PositiveOrZero(message = "El stock minimo no puede ser negativo")
    private BigDecimal minimumStock;
    private LocalDateTime lastAlertedAt;
    private LocalDateTime updatedAt;
}
