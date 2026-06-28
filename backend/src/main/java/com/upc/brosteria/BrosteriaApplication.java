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
            // Asegurar que los roles basicos existen
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

            // Leer contraseñas de las variables de entorno o usar la default de respaldo
            String envAdminPass = System.getenv("ADMIN_PASSWORD");
            String envCajeroPass = System.getenv("CAJERO_PASSWORD");

            String adminPassword = (envAdminPass != null && !envAdminPass.trim().isEmpty()) ? envAdminPass.trim() : "BrosteriaCRM2026!";
            String cajeroPassword = (envCajeroPass != null && !envCajeroPass.trim().isEmpty()) ? envCajeroPass.trim() : "BrosteriaCRM2026!";

            // Asegurar los 5 usuarios requeridos (2 Admins y 3 Cajeros de Atencion)
            crearUsuarioSiNoExiste(usuarioRepositorio, passwordEncoder, "admin@brosteria.com", "Josue Espinoza (Admin 1)", adminPassword, adminRol);
            crearUsuarioSiNoExiste(usuarioRepositorio, passwordEncoder, "admin2@brosteria.com", "Administrador 2", adminPassword, adminRol);
            crearUsuarioSiNoExiste(usuarioRepositorio, passwordEncoder, "cajero@brosteria.com", "Carlos Cajero (Cajero 1)", cajeroPassword, cajeroRol);
            crearUsuarioSiNoExiste(usuarioRepositorio, passwordEncoder, "cajero2@brosteria.com", "Atencion 2", cajeroPassword, cajeroRol);
            crearUsuarioSiNoExiste(usuarioRepositorio, passwordEncoder, "cajero3@brosteria.com", "Atencion 3", cajeroPassword, cajeroRol);
        };
    }

    private void crearUsuarioSiNoExiste(UsuarioRepositorio usuarioRepositorio, PasswordEncoder passwordEncoder,
                                         String email, String name, String password, RolEntidad rol) {
        java.util.Optional<UsuarioEntidad> optUsuario = usuarioRepositorio.findByEmail(email);
        if (optUsuario.isEmpty()) {
            UsuarioEntidad usuario = new UsuarioEntidad();
            usuario.setEmail(email);
            usuario.setName(name);
            usuario.setPasswordHash(passwordEncoder.encode(password));
            usuario.setRolEntidad(rol);
            usuarioRepositorio.save(usuario);
            System.out.println("Usuario de personal creado (nuevo): " + email);
        } else {
            // Si ya existe, podemos actualizar la contraseña si y solo si la contraseña del entorno es distinta
            // de la por defecto, para permitir rotaciones forzadas desde variables de entorno.
            String envAdminPass = System.getenv("ADMIN_PASSWORD");
            String envCajeroPass = System.getenv("CAJERO_PASSWORD");
            boolean overrideRequested = (rol.getName().equals("ADMIN") && envAdminPass != null && !envAdminPass.trim().isEmpty())
                                     || (rol.getName().equals("CAJERO") && envCajeroPass != null && !envCajeroPass.trim().isEmpty());

            if (overrideRequested) {
                UsuarioEntidad usuario = optUsuario.get();
                usuario.setPasswordHash(passwordEncoder.encode(password));
                usuarioRepositorio.save(usuario);
                System.out.println("Contraseña de usuario rotada por variable de entorno: " + email);
            }
        }
    }
}
