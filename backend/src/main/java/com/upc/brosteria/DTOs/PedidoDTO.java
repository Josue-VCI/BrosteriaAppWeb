package com.upc.brosteria.DTOs;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class PedidoDTO {
    private Long id;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private Double deliveryCost;
    private String type;
    private String paymentMethod;
    private Double total;
    private String status;
    private Long clienteId;
    private LocalDateTime orderDate;
    private List<DetallePedidoDTO> detalles;
}
