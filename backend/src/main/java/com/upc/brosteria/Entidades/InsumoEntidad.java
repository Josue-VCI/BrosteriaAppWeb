package com.upc.brosteria.Entidades;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.ZoneOffset;

@Entity
@Table(name = "insumos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class InsumoEntidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false)
    private String unit; // unidades, kg, litros, galones

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal minimumStock;

    @Column
    private LocalDateTime lastAlertedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
