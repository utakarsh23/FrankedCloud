package com.shresth.FrankenCloud.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WHAT: Data Transfer Object (DTO) for returning JWT authentication response payload.
 * HOW: Wraps the generated JWT token string along with token type metadata.
 * WHY: Standardizes the response structure returned to client upon successful login or registration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** 
     * WHAT: The compact signed JWT string.
     * HOW: Generated via Jwts.builder() using HMAC-SHA key.
     * WHY: The client attaches this token in the 'Authorization: Bearer <token>' header for subsequent requests.
     */
    private String token;

    /** 
     * WHAT: Token scheme type.
     * HOW: Always defaults to "Bearer".
     * WHY: Informs HTTP client how to attach the token in standard Authorization header.
     */
    private String tokenType = "Bearer";

    /** Username associated with the token */
    private String username;

    /** User email associated with the token */
    private String email;

    public AuthResponse(String token, String username, String email) {
        this.token = token;
        this.username = username;
        this.email = email;
        this.tokenType = "Bearer";
    }
}
