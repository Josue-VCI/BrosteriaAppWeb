package com.upc.brosteria.Repositorios;

import com.upc.brosteria.Entidades.InsumoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InsumoRepositorio extends JpaRepository<InsumoEntidad, Long> {
}
