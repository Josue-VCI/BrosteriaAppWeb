package com.upc.brosteria.Entidades;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.ZoneOffset;

@Entity
@Table(name = "pedidos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class PedidoEntidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 64)
    private String requestId;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String customerPhone;

    @Column
    private String customerAddress;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal deliveryCost;

    @Column(nullable = false)
    private String type; // DELIVERY, PICKUP

    @Column(nullable = false)
    private String paymentMethod; // YAPE, PLIN, EFECTIVO

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(nullable = false)
    private String status; // PENDIENTE, PREPARANDO, ENVIADO, ENTREGADO, CANCELADO

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private ClienteEntidad clienteEntidad;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @PrePersist
    protected void onCreate() {
        if (orderDate == null) {
            orderDate = LocalDateTime.now(ZoneOffset.UTC);
        }
    }
}
