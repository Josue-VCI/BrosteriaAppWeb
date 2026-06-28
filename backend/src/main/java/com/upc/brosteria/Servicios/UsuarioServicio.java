package com.upc.brosteria.Servicios;

import com.upc.brosteria.DTOs.LoginRequest;
import com.upc.brosteria.DTOs.LoginResponse;
import com.upc.brosteria.Excepciones.CredencialesInvalidasException;
import com.upc.brosteria.Entidades.UsuarioEntidad;
import com.upc.brosteria.Repositorios.UsuarioRepositorio;
import com.upc.brosteria.Seguridad.JwtUtil;
import com.upc.brosteria.Seguridad.UsuarioDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioServicio {

    private static final String HASH_FICTICIO = "$2a$10$7EqJtq98hPqEX7fNZaFWoO5ZExrXvqfY3H6C8H4e6R8aA6M8qYf2K";

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioDetailsService usuarioDetailsService;

    public LoginResponse login(LoginRequest loginRequest) {
        // Encontrar el usuario en la BD
        java.util.Optional<UsuarioEntidad> usuarioEncontrado = usuarioRepositorio.findByEmail(loginRequest.getEmail().trim().toLowerCase());
        String hash = usuarioEncontrado.map(UsuarioEntidad::getPasswordHash).orElse(HASH_FICTICIO);

        if (passwordEncoder.matches(loginRequest.getPassword(), hash) && usuarioEncontrado.isPresent()) {
            UsuarioEntidad usuario = usuarioEncontrado.get();
            // Cargar UserDetails para generar token con el rol
            UserDetails userDetails = usuarioDetailsService.loadUserByUsername(usuario.getEmail());
            String roleName = usuario.getRolEntidad().getName();
            String token = jwtUtil.generateToken(userDetails, roleName);
            
            return new LoginResponse(token, usuario.getId(), usuario.getName(), roleName);
        }
        throw new CredencialesInvalidasException();
    }
}
