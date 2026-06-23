package com.upc.brosteria.Repositorios;

import com.upc.brosteria.Entidades.ClienteEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ClienteRepositorio extends JpaRepository<ClienteEntidad, Long> {
    Optional<ClienteEntidad> findByPhone(String phone);
    Optional<ClienteEntidad> findByEmail(String email);
}
