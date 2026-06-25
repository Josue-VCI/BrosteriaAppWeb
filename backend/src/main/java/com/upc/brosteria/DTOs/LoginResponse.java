package com.upc.brosteria.DTOs;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class LoginResponse {
    private String token;
    private Long userId;
    private String userName;
    private String role;
}
