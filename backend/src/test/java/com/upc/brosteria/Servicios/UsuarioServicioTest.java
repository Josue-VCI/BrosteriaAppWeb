package com.upc.brosteria.Servicios;

import com.upc.brosteria.DTOs.LoginRequest;
import com.upc.brosteria.Excepciones.CredencialesInvalidasException;
import com.upc.brosteria.Repositorios.UsuarioRepositorio;
import com.upc.brosteria.Seguridad.JwtUtil;
import com.upc.brosteria.Seguridad.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServicioTest {

    @Mock private UsuarioRepositorio usuarioRepositorio;
    @Mock private JwtUtil jwtUtil;
    @Mock private UsuarioDetailsService usuarioDetailsService;
    @Spy private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks private UsuarioServicio usuarioServicio;

    @Test
    void noRevelaSiElCorreoExiste() {
        when(usuarioRepositorio.findByEmail("desconocido@correo.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest("desconocido@correo.com", "clave-invalida");

        assertThrows(CredencialesInvalidasException.class, () -> usuarioServicio.login(request));
    }
}
