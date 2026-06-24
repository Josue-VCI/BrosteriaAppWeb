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
}
