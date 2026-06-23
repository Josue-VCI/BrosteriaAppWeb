package com.upc.brosteria.DTOs;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ProductoDTO {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private String category;
    private String imageUrl;
    private Boolean active;
}
