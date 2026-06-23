package com.upc.brosteria.Entidades;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "detalle_pedidos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class DetallePedidoEntidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoEntidad pedidoEntidad;

    @ManyToOne
    @JoinColumn(name = "producto_id", nullable = false)
    private ProductoEntidad productoEntidad;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double subtotal;

    @Column
    private String creams; // Lista de cremas seleccionadas por ejemplo: "Mayonesa, Ají, Ketchup"
}
