package com.upc.brosteria.DTOs;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ClienteDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private Integer totalOrders;
    private Double totalSpent;
    private Integer points;
    private LocalDateTime createdAt;
}
