package com.shresth.FrankenCloud.Services;

import com.shresth.FrankenCloud.Config.JwtUtils;
import com.shresth.FrankenCloud.DTO.AuthResponse;
import com.shresth.FrankenCloud.Entity.User;
import com.shresth.FrankenCloud.Repositories.UserRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * WHAT: Core Business Service handling user accounts, password hashing, and JWT token issuance.
 * HOW: Embeds user ObjectId inside JWT claims upon registration and login.
 * WHY: Decouples frontend from needing to send userId in request payloads.
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailServiceImpl UserDetailServiceImpl;

    public User getById(ObjectId userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public User getByEmail(String email) {
        return userRepository.findUserByEmail(email);
    }

    /**
     * WHAT: Registers a new user and generates JWT token containing userId.
     * HOW: Saves user, fetches userDetails, and passes user.getId().toString() into jwtUtils.generateToken.
     */
    public AuthResponse registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);

        String identifier = savedUser.getUsername() != null ? savedUser.getUsername() : savedUser.getEmail();
        UserDetails userDetails = UserDetailServiceImpl.loadUserByUsername(identifier);

        // Include userId in JWT token claims
        String jwtToken = jwtUtils.generateToken(userDetails, savedUser.getId().toString());

        return new AuthResponse(jwtToken, savedUser.getUsername(), savedUser.getEmail());
    }

    /**
     * WHAT: Authenticates user credentials and generates JWT token containing userId.
     * HOW: Verifies BCrypt password, fetches dbUser to retrieve ObjectId, and generates JWT with userId claim.
     */
    public AuthResponse loginUser(String identifier, String rawPassword) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifier, rawPassword)
        );

        UserDetails userDetails = UserDetailServiceImpl.loadUserByUsername(identifier);
        User dbUser = existingUser(identifier, identifier);

        String userIdStr = dbUser != null ? dbUser.getId().toString() : "";

        // Include userId in JWT token claims
        String jwtToken = jwtUtils.generateToken(userDetails, userIdStr);

        return new AuthResponse(
                jwtToken,
                dbUser != null ? dbUser.getUsername() : identifier,
                dbUser != null ? dbUser.getEmail() : identifier
        );
    }

    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User existingUser(String email, String username) {
        return userRepository.findUserByEmailOrUsername(email, username);
    }

    public Boolean passwordMatch(String userPassword, String existingPassword) {
        return passwordEncoder.matches(userPassword, existingPassword);
    }
}
