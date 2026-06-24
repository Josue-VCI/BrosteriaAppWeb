package com.upc.brosteria.Repositorios;

import com.upc.brosteria.Entidades.DetallePedidoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DetallePedidoRepositorio extends JpaRepository<DetallePedidoEntidad, Long> {
    List<DetallePedidoEntidad> findByPedidoEntidadId(Long pedidoId);

    @Query("SELECT d FROM DetallePedidoEntidad d JOIN FETCH d.productoEntidad WHERE d.pedidoEntidad.id IN :pedidoIds")
    List<DetallePedidoEntidad> findByPedidoEntidadIdIn(@Param("pedidoIds") List<Long> pedidoIds);
}
