package com.upc.brosteria.DTOs;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class DetallePedidoDTO {
    private Long id;
    private Long productoId;
    private String productoName;
    private Double productoPrice;
    private Integer quantity;
    private Double subtotal;
    private String creams;
}
