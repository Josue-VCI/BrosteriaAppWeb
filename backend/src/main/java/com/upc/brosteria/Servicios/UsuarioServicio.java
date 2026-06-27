package com.upc.brosteria.Servicios;

import com.upc.brosteria.DTOs.LoginRequest;
import com.upc.brosteria.DTOs.LoginResponse;
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
        UsuarioEntidad usuario = usuarioRepositorio.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con el correo ingresado"));

        // Comparacion real con BCrypt
        if (passwordEncoder.matches(loginRequest.getPassword(), usuario.getPasswordHash())) {
            // Cargar UserDetails para generar token con el rol
            UserDetails userDetails = usuarioDetailsService.loadUserByUsername(usuario.getEmail());
            String roleName = usuario.getRolEntidad().getName();
            String token = jwtUtil.generateToken(userDetails, roleName);
            
            return new LoginResponse(token, usuario.getId(), usuario.getName(), roleName);
        } else {
            throw new RuntimeException("Contraseña incorrecta");
        }
    }
}
