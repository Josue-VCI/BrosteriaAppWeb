package com.upc.brosteria.Repositorios;

import com.upc.brosteria.Entidades.UsuarioEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;

@Repository
public interface UsuarioRepositorio extends JpaRepository<UsuarioEntidad, Long> {
    @EntityGraph(attributePaths = "rolEntidad")
    Optional<UsuarioEntidad> findByEmail(String email);
}
