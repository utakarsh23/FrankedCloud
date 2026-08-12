package com.shresth.FrankenCloud.Controllers;

import com.shresth.FrankenCloud.DTO.AuthRequest;
import com.shresth.FrankenCloud.DTO.AuthResponse;
import com.shresth.FrankenCloud.Entity.User;
import com.shresth.FrankenCloud.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;


    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        try {
            if (request.getEmail() == null || request.getUsername() == null || request.getPassword() == null) {
                return ResponseEntity.badRequest().body("Email, Username and Password are required");
            }

            User existingUser = userService.existingUser(request.getEmail(), request.getUsername());
            if (existingUser != null) {
                return ResponseEntity.badRequest().body("Email or Username already exists");
            }

            User newUser = new User();
            newUser.setEmail(request.getEmail());
            newUser.setUsername(request.getUsername());
            newUser.setPassword(request.getPassword());

            AuthResponse response = userService.registerUser(newUser);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error registering user: " + e.getMessage());
        }
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            String identifier = request.getEmail() != null ? request.getEmail() : request.getUsername();
            if (identifier == null || request.getPassword() == null) {
                return ResponseEntity.badRequest().body("Email or Username and Password are required");
            }

            AuthResponse response = userService.loginUser(identifier, request.getPassword());
            return ResponseEntity.ok(response);

        } catch (Exception loginError) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication failed: " + loginError.getMessage());
        }
    }
}
