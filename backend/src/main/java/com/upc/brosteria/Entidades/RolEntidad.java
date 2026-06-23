package com.upc.brosteria.Entidades;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class RolEntidad {
    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}
