package com.upc.brosteria.Entidades;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "productos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ProductoEntidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private String category; // COMBOS, CLASICOS, SALCHIPAPAS, BURGERS, ALITAS, BEBIDAS, EXTRAS

    @Column
    private String imageUrl;

    @Column(nullable = false)
    private Boolean active = true;
}
