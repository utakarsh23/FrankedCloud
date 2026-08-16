package com.shresth.FrankenCloud.Config;

import com.shresth.FrankenCloud.Services.UserDetailServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;

    @Autowired
    private UserDetailServiceImpl userDetailsService;

    /**
     * WHAT: Configures the main SecurityFilterChain bean.
     * HOW: Uses lambda DSL to define URL authorization, session creation policy, and custom filter placement.
     * WHY: Centralizes security control rules across all HTTP endpoints.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Step 1: Enable CORS & Disable CSRF
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)

                // Step 2: Configure Endpoint Authorization Rules
                // WHY: Specifies which API endpoints are public vs which require authenticated JWT sessions.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/actuator/**").permitAll() // Allow public registration/login & health checks
                        .anyRequest().authenticated()                             // Enforce authentication on all other endpoints
                )

                // Step 3: Enforce Stateless Session Policy
                // WHY: Tells Spring Security NEVER to create or store server-side HttpSession objects (JSESSIONID). Each request must supply a valid JWT.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Step 4: Register Custom Authentication Provider
                // WHY: Connects UserDetailsService and PasswordEncoder to authenticate login requests.
                .authenticationProvider(authenticationProvider())

                // Step 5: Inject Custom JwtAuthenticationFilter Before Spring's UsernamePasswordAuthenticationFilter
                // WHY: Intercepts incoming HTTP requests to validate JWT token before Spring attempts default session/form authentication.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * WHAT: Configures DaoAuthenticationProvider bean.
     * HOW: Binds UserDetailServiceImpl and PasswordEncoder together.
     * WHY: Used by AuthenticationManager during login to fetch user from DB and compare BCrypt password hashes.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * WHAT: Exposes the Spring AuthenticationManager bean.
     * HOW: Fetches authentication manager from Spring's AuthenticationConfiguration.
     * WHY: Allows AuthController / UserService to programmatically authenticate user credentials during login.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * WHAT: Exposes BCryptPasswordEncoder bean.
     * HOW: Returns standard BCryptPasswordEncoder instance.
     * WHY: Provides password hashing mechanism to encrypt passwords on registration and check hashes on login.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
