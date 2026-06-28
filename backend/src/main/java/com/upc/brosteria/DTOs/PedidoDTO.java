package com.upc.brosteria.DTOs;

import lombok.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class PedidoDTO {
    private Long id;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private String customerEmail;
    private Double deliveryCost;

    @NotNull(message = "El tipo de entrega no puede ser nulo")
    private String type;

    @NotNull(message = "El metodo de pago no puede ser nulo")
    private String paymentMethod;

    private Double total;
    private String status;
    private Long clienteId;
    private LocalDateTime orderDate;

    @NotEmpty(message = "El pedido debe contener al menos un detalle de producto")
    @Valid
    private List<DetallePedidoDTO> detalles;
}
