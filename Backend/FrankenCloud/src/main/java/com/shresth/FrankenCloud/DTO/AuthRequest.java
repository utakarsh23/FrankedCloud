package com.shresth.FrankenCloud.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WHAT: Data Transfer Object (DTO) for receiving user authentication credentials (login/register requests).
 * HOW: Encapsulates incoming JSON payload fields (email, username, password) sent by the client.
 * WHY: Keeps Entity models decoupled from API payload structures and prevents exposing database internal fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    /** User email address used for login or registration */
    private String email;

    /** Unique username used for identification */
    private String username;

    /** Plaintext password sent by client (will be hashed prior to DB storage) */
    private String password;
}
