package com.upc.brosteria.Entidades;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class PedidoEntidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String customerPhone;

    @Column
    private String customerAddress;

    @Column(nullable = false)
    private Double deliveryCost;

    @Column(nullable = false)
    private String type; // DELIVERY, PICKUP

    @Column(nullable = false)
    private String paymentMethod; // YAPE, PLIN, EFECTIVO

    @Column(nullable = false)
    private Double total;

    @Column(nullable = false)
    private String status; // PENDIENTE, PREPARANDO, ENVIADO, ENTREGADO, CANCELADO

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private ClienteEntidad clienteEntidad;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @PrePersist
    protected void onCreate() {
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }
    }
}
