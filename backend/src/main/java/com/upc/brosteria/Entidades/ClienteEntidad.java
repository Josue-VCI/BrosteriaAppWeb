package com.upc.brosteria.Entidades;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "clientes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ClienteEntidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column
    private String address;

    @Column(columnDefinition = "integer default 0")
    private Integer totalOrders = 0;

    @Column(precision = 14, scale = 2)
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(columnDefinition = "integer default 0")
    private Integer points = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
