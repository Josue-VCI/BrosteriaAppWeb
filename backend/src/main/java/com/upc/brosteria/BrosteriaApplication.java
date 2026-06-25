package com.upc.brosteria;

import com.upc.brosteria.Entidades.RolEntidad;
import com.upc.brosteria.Entidades.UsuarioEntidad;
import com.upc.brosteria.Repositorios.RolRepositorio;
import com.upc.brosteria.Repositorios.UsuarioRepositorio;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableAsync
public class BrosteriaApplication {

    public static void main(String[] args) {
        SpringApplication.run(BrosteriaApplication.class, args);
    }

    @Bean
    public CommandLineRunner initPasswords(UsuarioRepositorio usuarioRepositorio, RolRepositorio rolRepositorio, PasswordEncoder passwordEncoder) {
        return args -> {
            // Asegurar que los roles básicos existen
            if (!rolRepositorio.existsById(1L)) {
                rolRepositorio.save(new RolEntidad(1L, "ADMIN"));
            }
            if (!rolRepositorio.existsById(2L)) {
                rolRepositorio.save(new RolEntidad(2L, "CAJERO"));
            }
            if (!rolRepositorio.existsById(3L)) {
                rolRepositorio.save(new RolEntidad(3L, "COCINERO"));
            }

            RolEntidad adminRol = rolRepositorio.findById(1L).orElseThrow();
            RolEntidad cajeroRol = rolRepositorio.findById(2L).orElseThrow();

            // Asegurar/Actualizar los 5 usuarios requeridos (2 Admins y 3 Cajeros de Atención)
            crearOActualizarUsuario(usuarioRepositorio, passwordEncoder, "admin@brosteria.com", "Josue Espinoza (Admin 1)", "admin_broster1", adminRol);
            crearOActualizarUsuario(usuarioRepositorio, passwordEncoder, "admin2@brosteria.com", "Administrador 2", "admin_broster2", adminRol);
            crearOActualizarUsuario(usuarioRepositorio, passwordEncoder, "cajero@brosteria.com", "Carlos Cajero (Cajero 1)", "caja_broster1", cajeroRol);
            crearOActualizarUsuario(usuarioRepositorio, passwordEncoder, "cajero2@brosteria.com", "Atención 2", "caja_broster2", cajeroRol);
            crearOActualizarUsuario(usuarioRepositorio, passwordEncoder, "cajero3@brosteria.com", "Atención 3", "caja_broster3", cajeroRol);
        };
    }

    private void crearOActualizarUsuario(UsuarioRepositorio usuarioRepositorio, PasswordEncoder passwordEncoder,
                                         String email, String name, String rawPassword, RolEntidad rol) {
        UsuarioEntidad usuario = usuarioRepositorio.findByEmail(email).orElse(new UsuarioEntidad());
        usuario.setEmail(email);
        usuario.setName(name);
        usuario.setPasswordHash(passwordEncoder.encode(rawPassword));
        usuario.setRolEntidad(rol);
        usuarioRepositorio.save(usuario);
        System.out.println("Usuario de personal asegurado/actualizado: " + email);
    }
}
