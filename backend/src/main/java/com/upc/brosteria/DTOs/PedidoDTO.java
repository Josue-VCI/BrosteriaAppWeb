package com.upc.brosteria.DTOs;

import lombok.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class PedidoDTO {
    private Long id;
    @Size(max = 120, message = "El nombre del cliente es demasiado largo")
    private String customerName;
    @Pattern(regexp = "^$|^[0-9+() -]{7,20}$", message = "El telefono del cliente no es valido")
    private String customerPhone;
    @Size(max = 250, message = "La direccion del cliente es demasiado larga")
    private String customerAddress;
    @Email(message = "El correo del cliente no es valido")
    @Size(max = 180, message = "El correo del cliente es demasiado largo")
    private String customerEmail;
    @PositiveOrZero(message = "El costo de envio no puede ser negativo")
    private Double deliveryCost;

    @NotNull(message = "El tipo de entrega no puede ser nulo")
    @Pattern(regexp = "DELIVERY|PICKUP", message = "El tipo de entrega no es valido")
    private String type;

    @NotNull(message = "El metodo de pago no puede ser nulo")
    @Pattern(regexp = "YAPE|PLIN|TARJETA|EFECTIVO", message = "El metodo de pago no es valido")
    private String paymentMethod;

    private Double total;
    private String status;
    private Long clienteId;
    private LocalDateTime orderDate;

    @NotEmpty(message = "El pedido debe contener al menos un detalle de producto")
    @Valid
    private List<DetallePedidoDTO> detalles;
}
