package com.upc.brosteria.Servicios;

import com.upc.brosteria.DTOs.LoginRequest;
import com.upc.brosteria.DTOs.LoginResponse;
import com.upc.brosteria.Entidades.UsuarioEntidad;
import com.upc.brosteria.Repositorios.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioServicio {

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    public LoginResponse login(LoginRequest loginRequest) {
        // Encontrar el usuario en la BD
        UsuarioEntidad usuario = usuarioRepositorio.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con el correo ingresado"));

        // Comparación simple de contraseña en texto plano para desarrollo (seguridad desactivada)
        if ("admin123".equals(loginRequest.getPassword()) || "cajero123".equals(loginRequest.getPassword())) {
            // Devolver un token mock estático libre de fallos criptográficos
            String tokenMock = "token_demo_brosteria_exitoso";
            return new LoginResponse(tokenMock, usuario.getId(), usuario.getName());
        } else {
            throw new RuntimeException("Contraseña incorrecta para el usuario de demostración");
        }
    }
}
