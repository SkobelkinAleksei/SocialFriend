package org.example.security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String refreshToken;
    private String tokenType;

    public JwtResponse(String token, String refreshToken) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.tokenType = "Bearer";
    }
}
