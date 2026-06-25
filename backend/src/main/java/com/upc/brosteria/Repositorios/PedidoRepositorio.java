package com.upc.brosteria.Repositorios;

import com.upc.brosteria.Entidades.PedidoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PedidoRepositorio extends JpaRepository<PedidoEntidad, Long> {
    
    @Query("SELECT p FROM PedidoEntidad p LEFT JOIN FETCH p.clienteEntidad")
    List<PedidoEntidad> findAllWithCliente();

    @Query("SELECT p FROM PedidoEntidad p LEFT JOIN FETCH p.clienteEntidad WHERE p.status = :status")
    List<PedidoEntidad> findByStatusWithCliente(@Param("status") String status);

    List<PedidoEntidad> findByStatus(String status);

    @Query("SELECT p FROM PedidoEntidad p LEFT JOIN FETCH p.clienteEntidad WHERE p.status IN ('PENDIENTE', 'PREPARANDO', 'ENVIADO') AND p.orderDate >= :since ORDER BY p.orderDate DESC")
    List<PedidoEntidad> findActiveWithCliente(@org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);

    @Query("SELECT p FROM PedidoEntidad p LEFT JOIN FETCH p.clienteEntidad ORDER BY p.orderDate DESC")
    List<PedidoEntidad> findRecentWithCliente(org.springframework.data.domain.Pageable pageable);

    long countByOrderDateBefore(java.time.LocalDateTime date);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM PedidoEntidad p WHERE p.orderDate < :date")
    void deleteByOrderDateBefore(@Param("date") java.time.LocalDateTime date);
}
