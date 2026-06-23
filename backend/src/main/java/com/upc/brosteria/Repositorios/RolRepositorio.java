package com.upc.brosteria.Repositorios;

import com.upc.brosteria.Entidades.RolEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolRepositorio extends JpaRepository<RolEntidad, Long> {
}
