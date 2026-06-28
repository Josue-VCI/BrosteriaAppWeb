package com.upc.brosteria;

import com.upc.brosteria.Entidades.RolEntidad;
import com.upc.brosteria.Entidades.UsuarioEntidad;
import com.upc.brosteria.Repositorios.RolRepositorio;
import com.upc.brosteria.Repositorios.UsuarioRepositorio;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Optional;

@SpringBootApplication
@EnableAsync
public class BrosteriaApplication {

    public static void main(String[] args) {
        SpringApplication.run(BrosteriaApplication.class, args);
    }

    @Bean
    public CommandLineRunner initPasswords(UsuarioRepositorio usuarioRepositorio,
                                           RolRepositorio rolRepositorio,
                                           PasswordEncoder passwordEncoder,
                                           Environment environment) {
        return args -> {
            boolean prodProfile = Arrays.asList(environment.getActiveProfiles()).contains("prod");

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

            String envAdminPass = System.getenv("ADMIN_PASSWORD");
            String envCajeroPass = System.getenv("CAJERO_PASSWORD");
            boolean adminPasswordConfigured = envAdminPass != null && !envAdminPass.trim().isEmpty();
            boolean cajeroPasswordConfigured = envCajeroPass != null && !envCajeroPass.trim().isEmpty();

            if (prodProfile && (!adminPasswordConfigured || !cajeroPasswordConfigured)) {
                System.out.println("Production profile detected: staff seed users were not created or rotated because ADMIN_PASSWORD and CAJERO_PASSWORD are required.");
                return;
            }

            String adminPassword = adminPasswordConfigured ? envAdminPass.trim() : "BrosteriaCRM2026!";
            String cajeroPassword = cajeroPasswordConfigured ? envCajeroPass.trim() : "BrosteriaCRM2026!";

            crearUsuarioSiNoExiste(usuarioRepositorio, passwordEncoder, "admin@brosteria.com", "Josue Espinoza (Admin 1)", adminPassword, adminRol, adminPasswordConfigured);
            crearUsuarioSiNoExiste(usuarioRepositorio, passwordEncoder, "admin2@brosteria.com", "Administrador 2", adminPassword, adminRol, adminPasswordConfigured);
            crearUsuarioSiNoExiste(usuarioRepositorio, passwordEncoder, "cajero@brosteria.com", "Carlos Cajero (Cajero 1)", cajeroPassword, cajeroRol, cajeroPasswordConfigured);
            crearUsuarioSiNoExiste(usuarioRepositorio, passwordEncoder, "cajero2@brosteria.com", "Atencion 2", cajeroPassword, cajeroRol, cajeroPasswordConfigured);
            crearUsuarioSiNoExiste(usuarioRepositorio, passwordEncoder, "cajero3@brosteria.com", "Atencion 3", cajeroPassword, cajeroRol, cajeroPasswordConfigured);
        };
    }

    private void crearUsuarioSiNoExiste(UsuarioRepositorio usuarioRepositorio,
                                         PasswordEncoder passwordEncoder,
                                         String email,
                                         String name,
                                         String password,
                                         RolEntidad rol,
                                         boolean passwordOverrideRequested) {
        Optional<UsuarioEntidad> optUsuario = usuarioRepositorio.findByEmail(email);
        if (optUsuario.isEmpty()) {
            UsuarioEntidad usuario = new UsuarioEntidad();
            usuario.setEmail(email);
            usuario.setName(name);
            usuario.setPasswordHash(passwordEncoder.encode(password));
            usuario.setRolEntidad(rol);
            usuarioRepositorio.save(usuario);
            System.out.println("Usuario de personal creado nuevo: " + email);
        } else if (passwordOverrideRequested) {
            UsuarioEntidad usuario = optUsuario.get();
            usuario.setPasswordHash(passwordEncoder.encode(password));
            usuarioRepositorio.save(usuario);
            System.out.println("Password de usuario rotada por variable de entorno: " + email);
        }
    }
}
