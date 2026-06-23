package com.upc.brosteria.Repositorios;

import com.upc.brosteria.Entidades.PedidoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PedidoRepositorio extends JpaRepository<PedidoEntidad, Long> {
    List<PedidoEntidad> findByStatus(String status);
}
