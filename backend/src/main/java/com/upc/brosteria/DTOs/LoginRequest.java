package com.upc.brosteria.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "El correo es requerido")
    @Email(message = "Debe ser un correo valido")
    private String email;

    @NotBlank(message = "La contrasena es requerida")
    private String password;
}
