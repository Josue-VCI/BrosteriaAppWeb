package com.upc.brosteria.Repositorios;

import com.upc.brosteria.Entidades.PedidoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PedidoRepositorio extends JpaRepository<PedidoEntidad, Long> {

    interface ResumenReporte {
        BigDecimal getVentasTotales();
        Long getTotalPedidos();
        Long getCompletados();
        Long getCancelados();
    }

    interface EtiquetaMonto {
        String getEtiqueta();
        BigDecimal getMonto();
    }

    interface EtiquetaConteo {
        String getEtiqueta();
        Long getCantidad();
    }

    interface HoraConteo {
        Integer getHora();
        Long getCantidad();
    }

    interface ProductoConteo {
        String getNombre();
        Long getCantidad();
    }

    interface EstadisticasCliente {
        Long getTotalPedidos();
        BigDecimal getTotalGastado();
    }

    Optional<PedidoEntidad> findByRequestId(String requestId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PedidoEntidad p WHERE p.id = :id")
    Optional<PedidoEntidad> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT p FROM PedidoEntidad p LEFT JOIN FETCH p.clienteEntidad ORDER BY p.orderDate DESC")
    List<PedidoEntidad> findAllWithCliente(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT p FROM PedidoEntidad p LEFT JOIN FETCH p.clienteEntidad WHERE p.status = :status")
    List<PedidoEntidad> findByStatusWithCliente(@Param("status") String status);

    @Query("SELECT p FROM PedidoEntidad p LEFT JOIN FETCH p.clienteEntidad WHERE p.status = :status ORDER BY p.orderDate DESC")
    List<PedidoEntidad> findByStatusWithCliente(@Param("status") String status,
                                                org.springframework.data.domain.Pageable pageable);

    @Query("SELECT p FROM PedidoEntidad p LEFT JOIN FETCH p.clienteEntidad WHERE p.status = :status AND p.orderDate >= :inicio AND p.orderDate < :fin ORDER BY p.orderDate DESC")
    List<PedidoEntidad> findByStatusAndRangeWithCliente(@Param("status") String status,
                                                        @Param("inicio") LocalDateTime inicio,
                                                        @Param("fin") LocalDateTime fin);

    @Query("SELECT p FROM PedidoEntidad p LEFT JOIN FETCH p.clienteEntidad WHERE p.status IN ('PENDIENTE', 'PREPARANDO', 'ENVIADO') AND p.orderDate >= :since ORDER BY p.orderDate DESC")
    List<PedidoEntidad> findActiveWithCliente(@org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);

    @Query("SELECT p FROM PedidoEntidad p LEFT JOIN FETCH p.clienteEntidad ORDER BY p.orderDate DESC")
    List<PedidoEntidad> findRecentWithCliente(org.springframework.data.domain.Pageable pageable);

    @Query(value = """
            SELECT COUNT(*) AS "totalPedidos", COALESCE(SUM(total), 0) AS "totalGastado"
            FROM pedidos
            WHERE customer_phone = :telefono AND payment_status = 'PAGADO'
            """, nativeQuery = true)
    EstadisticasCliente obtenerEstadisticasCliente(@Param("telefono") String telefono);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "UPDATE pedidos SET cliente_id = :clienteId WHERE customer_phone = :telefono AND cliente_id IS DISTINCT FROM :clienteId", nativeQuery = true)
    int vincularPedidosPorTelefono(@Param("clienteId") Long clienteId, @Param("telefono") String telefono);

    @Query(value = """
            SELECT (SELECT COALESCE(SUM(p2.total), 0) FROM pedidos p2 WHERE p2.payment_status = 'PAGADO' AND p2.paid_at BETWEEN :inicio AND :fin AND (CAST(:tipo AS text) = '' OR UPPER(p2.type) = UPPER(CAST(:tipo AS text))) AND (CAST(:dia AS integer) = 0 OR EXTRACT(ISODOW FROM p2.paid_at - INTERVAL '5 hours') = CAST(:dia AS integer))) AS "ventasTotales",
                   COUNT(*) AS "totalPedidos",
                   COUNT(*) FILTER (WHERE status = 'ENTREGADO') AS completados,
                   COUNT(*) FILTER (WHERE status = 'CANCELADO') AS cancelados
            FROM pedidos
            WHERE order_date BETWEEN :inicio AND :fin
              AND (CAST(:tipo AS text) = '' OR UPPER(type) = UPPER(CAST(:tipo AS text)))
              AND (CAST(:dia AS integer) = 0 OR EXTRACT(ISODOW FROM order_date - INTERVAL '5 hours') = CAST(:dia AS integer))
            """, nativeQuery = true)
    ResumenReporte obtenerResumenReporte(@Param("inicio") LocalDateTime inicio,
                                          @Param("fin") LocalDateTime fin,
                                          @Param("tipo") String tipo,
                                          @Param("dia") int dia);

    @Query(value = """
            SELECT TO_CHAR(paid_at - INTERVAL '5 hours', 'YYYY-MM-DD') AS etiqueta,
                   SUM(total) AS monto
            FROM pedidos
            WHERE payment_status = 'PAGADO' AND paid_at BETWEEN :inicio AND :fin
              AND (CAST(:tipo AS text) = '' OR UPPER(type) = UPPER(CAST(:tipo AS text)))
              AND (CAST(:dia AS integer) = 0 OR EXTRACT(ISODOW FROM paid_at - INTERVAL '5 hours') = CAST(:dia AS integer))
            GROUP BY 1 ORDER BY 1
            """, nativeQuery = true)
    List<EtiquetaMonto> ventasPorFecha(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin,
                                       @Param("tipo") String tipo, @Param("dia") int dia);

    @Query(value = """
            SELECT CASE
                     WHEN UPPER(payment_method) = 'EFECTIVO' THEN 'EFECTIVO'
                     WHEN UPPER(payment_method) = 'YAPE' THEN 'YAPE'
                     ELSE 'OTRO'
                   END AS etiqueta,
                   COUNT(*) AS cantidad
            FROM pedidos
            WHERE payment_status = 'PAGADO' AND paid_at BETWEEN :inicio AND :fin
              AND (CAST(:tipo AS text) = '' OR UPPER(type) = UPPER(CAST(:tipo AS text)))
              AND (CAST(:dia AS integer) = 0 OR EXTRACT(ISODOW FROM paid_at - INTERVAL '5 hours') = CAST(:dia AS integer))
            GROUP BY 1
            """, nativeQuery = true)
    List<EtiquetaConteo> pagosReporte(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin,
                                      @Param("tipo") String tipo, @Param("dia") int dia);

    @Query(value = """
            SELECT EXTRACT(HOUR FROM order_date - INTERVAL '5 hours')::int AS hora, COUNT(*) AS cantidad
            FROM pedidos
            WHERE status = 'ENTREGADO' AND order_date BETWEEN :inicio AND :fin
              AND (CAST(:tipo AS text) = '' OR UPPER(type) = UPPER(CAST(:tipo AS text)))
              AND (CAST(:dia AS integer) = 0 OR EXTRACT(ISODOW FROM order_date - INTERVAL '5 hours') = CAST(:dia AS integer))
            GROUP BY 1 ORDER BY 1
            """, nativeQuery = true)
    List<HoraConteo> pedidosPorHora(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin,
                                    @Param("tipo") String tipo, @Param("dia") int dia);

    @Query(value = """
            SELECT CASE
                     WHEN customer_address IS NULL OR TRIM(customer_address) = '' THEN 'Sin Direccion'
                     WHEN LOWER(customer_address) LIKE 'retiro en local%' THEN 'Retiro en Local'
                     WHEN LOWER(customer_address) LIKE '%surquillo%' THEN 'Surquillo'
                     WHEN LOWER(customer_address) LIKE '%carabayllo%' THEN 'Carabayllo'
                     WHEN LOWER(customer_address) LIKE '%comas%' THEN 'Comas'
                     WHEN LOWER(customer_address) LIKE '%surco%' THEN 'Surco'
                     WHEN LOWER(customer_address) LIKE '%miraflores%' THEN 'Miraflores'
                     WHEN LOWER(customer_address) LIKE '%san miguel%' THEN 'San Miguel'
                     WHEN LOWER(customer_address) LIKE '%magdalena%' THEN 'Magdalena'
                     ELSE 'Otros'
                   END AS etiqueta,
                   COUNT(*) AS cantidad
            FROM pedidos
            WHERE status = 'ENTREGADO' AND order_date BETWEEN :inicio AND :fin
              AND (CAST(:tipo AS text) = '' OR UPPER(type) = UPPER(CAST(:tipo AS text)))
              AND (CAST(:dia AS integer) = 0 OR EXTRACT(ISODOW FROM order_date - INTERVAL '5 hours') = CAST(:dia AS integer))
            GROUP BY 1 ORDER BY cantidad DESC
            """, nativeQuery = true)
    List<EtiquetaConteo> distritosReporte(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin,
                                          @Param("tipo") String tipo, @Param("dia") int dia);

    @Query(value = """
            SELECT pr.name AS nombre, SUM(dp.quantity)::bigint AS cantidad
            FROM detalle_pedidos dp
            JOIN pedidos p ON p.id = dp.pedido_id
            JOIN productos pr ON pr.id = dp.producto_id
            WHERE p.status = 'ENTREGADO' AND p.order_date BETWEEN :inicio AND :fin
              AND (CAST(:tipo AS text) = '' OR UPPER(p.type) = UPPER(CAST(:tipo AS text)))
              AND (CAST(:dia AS integer) = 0 OR EXTRACT(ISODOW FROM p.order_date - INTERVAL '5 hours') = CAST(:dia AS integer))
            GROUP BY pr.name ORDER BY cantidad DESC LIMIT 5
            """, nativeQuery = true)
    List<ProductoConteo> topProductosReporte(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin,
                                             @Param("tipo") String tipo, @Param("dia") int dia);

    @Query(value = """
            SELECT pr.name AS nombre, SUM(dp.quantity)::bigint AS cantidad
            FROM detalle_pedidos dp
            JOIN pedidos p ON p.id = dp.pedido_id
            JOIN productos pr ON pr.id = dp.producto_id
            WHERE p.status = 'ENTREGADO'
            GROUP BY pr.name ORDER BY cantidad DESC LIMIT 5
            """, nativeQuery = true)
    List<ProductoConteo> topProductosHistorico();

    @Query(value = """
            SELECT * FROM pedidos
            WHERE order_date BETWEEN :inicio AND :fin
              AND (CAST(:tipo AS text) = '' OR UPPER(type) = UPPER(CAST(:tipo AS text)))
              AND (CAST(:dia AS integer) = 0 OR EXTRACT(ISODOW FROM order_date - INTERVAL '5 hours') = CAST(:dia AS integer))
            ORDER BY order_date DESC
            """, nativeQuery = true)
    List<PedidoEntidad> buscarParaReporte(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin,
                                          @Param("tipo") String tipo, @Param("dia") int dia);

    long countByOrderDateBefore(java.time.LocalDateTime date);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM PedidoEntidad p WHERE p.orderDate < :date")
    void deleteByOrderDateBefore(@Param("date") java.time.LocalDateTime date);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE PedidoEntidad p SET p.clienteEntidad = null WHERE p.clienteEntidad.id = :clienteId")
    void detachCliente(@Param("clienteId") Long clienteId);
}
