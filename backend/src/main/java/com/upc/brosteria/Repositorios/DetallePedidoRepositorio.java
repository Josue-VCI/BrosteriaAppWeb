package com.upc.brosteria.Repositorios;

import com.upc.brosteria.Entidades.DetallePedidoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DetallePedidoRepositorio extends JpaRepository<DetallePedidoEntidad, Long> {
    List<DetallePedidoEntidad> findByPedidoEntidadId(Long pedidoId);
}
