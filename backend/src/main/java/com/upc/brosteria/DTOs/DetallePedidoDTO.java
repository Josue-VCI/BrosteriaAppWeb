package com.upc.brosteria.DTOs;

import lombok.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class DetallePedidoDTO {
    private Long id;

    @NotNull(message = "El ID de producto es obligatorio")
    private Long productoId;

    private String productoName;
    private Double productoPrice;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad minima de producto debe ser 1")
    private Integer quantity;

    private Double subtotal;
    private String creams;
}
