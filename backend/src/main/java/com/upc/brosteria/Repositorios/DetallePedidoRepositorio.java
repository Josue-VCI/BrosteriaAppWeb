package com.upc.brosteria.Repositorios;

import com.upc.brosteria.Entidades.DetallePedidoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DetallePedidoRepositorio extends JpaRepository<DetallePedidoEntidad, Long> {
    @Query("SELECT d FROM DetallePedidoEntidad d JOIN FETCH d.productoEntidad JOIN FETCH d.pedidoEntidad WHERE d.pedidoEntidad.id = :pedidoId")
    List<DetallePedidoEntidad> findByPedidoEntidadId(@Param("pedidoId") Long pedidoId);

    @Query("SELECT d FROM DetallePedidoEntidad d JOIN FETCH d.productoEntidad JOIN FETCH d.pedidoEntidad WHERE d.pedidoEntidad.id IN :pedidoIds")
    List<DetallePedidoEntidad> findByPedidoEntidadIdIn(@Param("pedidoIds") List<Long> pedidoIds);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM DetallePedidoEntidad d WHERE d.pedidoEntidad.id IN (SELECT p.id FROM PedidoEntidad p WHERE p.orderDate < :date)")
    void deleteByPedidoOrderDateBefore(@Param("date") java.time.LocalDateTime date);
}
