package com.upc.brosteria.Repositorios;

import com.upc.brosteria.Entidades.PedidoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    
    @Query("SELECT p FROM PedidoEntidad p LEFT JOIN FETCH p.clienteEntidad")
    List<PedidoEntidad> findAllWithCliente();

    List<PedidoEntidad> findByCustomerPhone(String customerPhone);

    @Query("SELECT p FROM PedidoEntidad p LEFT JOIN FETCH p.clienteEntidad WHERE p.status = :status")
    List<PedidoEntidad> findByStatusWithCliente(@Param("status") String status);

    List<PedidoEntidad> findByStatus(String status);

    @Query("SELECT p FROM PedidoEntidad p LEFT JOIN FETCH p.clienteEntidad WHERE p.status IN ('PENDIENTE', 'PREPARANDO', 'ENVIADO') AND p.orderDate >= :since ORDER BY p.orderDate DESC")
    List<PedidoEntidad> findActiveWithCliente(@org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);

    @Query("SELECT p FROM PedidoEntidad p LEFT JOIN FETCH p.clienteEntidad ORDER BY p.orderDate DESC")
    List<PedidoEntidad> findRecentWithCliente(org.springframework.data.domain.Pageable pageable);

    @Query(value = """
            SELECT COALESCE(SUM(total) FILTER (WHERE status = 'ENTREGADO'), 0) AS "ventasTotales",
                   COUNT(*) AS "totalPedidos",
                   COUNT(*) FILTER (WHERE status = 'ENTREGADO') AS completados,
                   COUNT(*) FILTER (WHERE status = 'CANCELADO') AS cancelados
            FROM pedidos
            WHERE order_date BETWEEN :inicio AND :fin
              AND (:tipo = '' OR UPPER(type) = UPPER(:tipo))
              AND (:dia = 0 OR EXTRACT(ISODOW FROM order_date - INTERVAL '5 hours') = :dia)
            """, nativeQuery = true)
    ResumenReporte obtenerResumenReporte(@Param("inicio") LocalDateTime inicio,
                                          @Param("fin") LocalDateTime fin,
                                          @Param("tipo") String tipo,
                                          @Param("dia") int dia);

    @Query(value = """
            SELECT TO_CHAR(order_date - INTERVAL '5 hours', 'YYYY-MM-DD') AS etiqueta,
                   SUM(total) AS monto
            FROM pedidos
            WHERE status = 'ENTREGADO' AND order_date BETWEEN :inicio AND :fin
              AND (:tipo = '' OR UPPER(type) = UPPER(:tipo))
              AND (:dia = 0 OR EXTRACT(ISODOW FROM order_date - INTERVAL '5 hours') = :dia)
            GROUP BY 1 ORDER BY 1
            """, nativeQuery = true)
    List<EtiquetaMonto> ventasPorFecha(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin,
                                       @Param("tipo") String tipo, @Param("dia") int dia);

    @Query(value = """
            SELECT payment_method AS etiqueta, COUNT(*) AS cantidad
            FROM pedidos
            WHERE status = 'ENTREGADO' AND order_date BETWEEN :inicio AND :fin
              AND (:tipo = '' OR UPPER(type) = UPPER(:tipo))
              AND (:dia = 0 OR EXTRACT(ISODOW FROM order_date - INTERVAL '5 hours') = :dia)
            GROUP BY payment_method
            """, nativeQuery = true)
    List<EtiquetaConteo> pagosReporte(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin,
                                      @Param("tipo") String tipo, @Param("dia") int dia);

    @Query(value = """
            SELECT EXTRACT(HOUR FROM order_date - INTERVAL '5 hours')::int AS hora, COUNT(*) AS cantidad
            FROM pedidos
            WHERE status = 'ENTREGADO' AND order_date BETWEEN :inicio AND :fin
              AND (:tipo = '' OR UPPER(type) = UPPER(:tipo))
              AND (:dia = 0 OR EXTRACT(ISODOW FROM order_date - INTERVAL '5 hours') = :dia)
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
              AND (:tipo = '' OR UPPER(type) = UPPER(:tipo))
              AND (:dia = 0 OR EXTRACT(ISODOW FROM order_date - INTERVAL '5 hours') = :dia)
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
              AND (:tipo = '' OR UPPER(p.type) = UPPER(:tipo))
              AND (:dia = 0 OR EXTRACT(ISODOW FROM p.order_date - INTERVAL '5 hours') = :dia)
            GROUP BY pr.name ORDER BY cantidad DESC LIMIT 5
            """, nativeQuery = true)
    List<ProductoConteo> topProductosReporte(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin,
                                             @Param("tipo") String tipo, @Param("dia") int dia);

    @Query(value = """
            SELECT * FROM pedidos
            WHERE order_date BETWEEN :inicio AND :fin
              AND (:tipo = '' OR UPPER(type) = UPPER(:tipo))
              AND (:dia = 0 OR EXTRACT(ISODOW FROM order_date - INTERVAL '5 hours') = :dia)
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
