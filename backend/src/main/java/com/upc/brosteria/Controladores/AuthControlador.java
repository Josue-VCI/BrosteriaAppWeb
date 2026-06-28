package com.upc.brosteria.Controladores;

import com.upc.brosteria.DTOs.LoginRequest;
import com.upc.brosteria.DTOs.LoginResponse;
import com.upc.brosteria.Servicios.UsuarioServicio;
import com.upc.brosteria.Excepciones.CredencialesInvalidasException;
import com.upc.brosteria.Seguridad.LoginRateLimitServicio;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthControlador {

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private LoginRateLimitServicio loginRateLimitServicio;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        loginRateLimitServicio.verificarPermitido(loginRequest.getEmail());
        try {
            LoginResponse response = usuarioServicio.login(loginRequest);
            loginRateLimitServicio.registrarExito(loginRequest.getEmail());
            return ResponseEntity.ok(response);
        } catch (CredencialesInvalidasException ex) {
            loginRateLimitServicio.registrarFallo(loginRequest.getEmail());
            throw ex;
        }
    }
}
