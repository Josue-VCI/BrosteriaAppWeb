package com.upc.brosteria;

import com.upc.brosteria.Entidades.UsuarioEntidad;
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
    public CommandLineRunner initPasswords(UsuarioRepositorio usuarioRepositorio, PasswordEncoder passwordEncoder) {
        return args -> {
            usuarioRepositorio.findByEmail("admin@brosteria.com").ifPresent(usuario -> {
                String hash = usuario.getPasswordHash();
                if (hash == null || !hash.startsWith("$2")) {
                    System.out.println("Auto-reparando contraseña de admin@brosteria.com a BCrypt...");
                    usuario.setPasswordHash(passwordEncoder.encode("admin123"));
                    usuarioRepositorio.save(usuario);
                }
            });

            usuarioRepositorio.findByEmail("cajero@brosteria.com").ifPresent(usuario -> {
                String hash = usuario.getPasswordHash();
                if (hash == null || !hash.startsWith("$2")) {
                    System.out.println("Auto-reparando contraseña de cajero@brosteria.com a BCrypt...");
                    usuario.setPasswordHash(passwordEncoder.encode("cajero123"));
                    usuarioRepositorio.save(usuario);
                }
            });
        };
    }
}
