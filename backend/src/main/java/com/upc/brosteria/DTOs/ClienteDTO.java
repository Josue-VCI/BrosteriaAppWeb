package com.upc.brosteria.DTOs;

import lombok.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ClienteDTO {
    private Long id;
    @NotBlank(message = "El nombre del cliente es requerido")
    @Size(max = 120, message = "El nombre del cliente es demasiado largo")
    private String name;
    @Email(message = "El correo del cliente no es valido")
    @Size(max = 180, message = "El correo del cliente es demasiado largo")
    private String email;
    @NotBlank(message = "El telefono del cliente es requerido")
    @Pattern(regexp = "^[0-9+() -]{7,20}$", message = "El telefono del cliente no es valido")
    private String phone;
    @Size(max = 250, message = "La direccion del cliente es demasiado larga")
    private String address;
    private Integer totalOrders;
    private BigDecimal totalSpent;
    private Integer points;
    private LocalDateTime createdAt;
}
