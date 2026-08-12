package com.shresth.FrankenCloud.Config;

import com.shresth.FrankenCloud.Services.UserDetailServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * WHAT: Custom HTTP Security Filter that intercepts incoming HTTP requests to process JWT authentication.
 * HOW: Extends OncePerRequestFilter to guarantee single execution per request dispatch. Extracts Bearer token, validates signature/expiration, and injects authentication token into SecurityContextHolder.
 * WHY: Enables stateless HTTP session management by authenticating requests dynamically based on JWT token rather than relying on server-side HTTP sessions (JSESSIONID).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailServiceImpl userDetailsService;

    /**
     * WHAT: Core filter logic executed for every incoming HTTP request.
     * HOW: Reads "Authorization" header, parses token, verifies validity, and builds SecurityContext authentication token.
     * WHY: Ensures protected REST API endpoints only process authorized requests containing valid, unexpired JWT tokens.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Step 1: Read the 'Authorization' HTTP request header
        final String authHeader = request.getHeader("Authorization");
        final String jwtToken;
        final String username;

        // Step 2: Check if header exists and follows the 'Bearer <token>' pattern
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // If missing or improper format, pass control down the filter chain (Spring Security will reject if endpoint requires auth)
            filterChain.doFilter(request, response);
            return;
        }

        // Step 3: Extract the raw JWT token string (skip "Bearer " prefix, which is 7 characters)
        jwtToken = authHeader.substring(7);

        try {
            // Step 4: Extract subject (username/email) from JWT token payload using JwtUtils
            username = jwtUtils.extractUsername(jwtToken);

            // Step 5: Validate user identity if username is present AND user is not already authenticated in current SecurityContext
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Step 6: Load UserDetails from database via UserDetailServiceImpl
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // Step 7: Verify token signature matching and check expiration
                if (jwtUtils.validateToken(jwtToken, userDetails)) {

                    // Step 8: Construct Spring Security Authentication Token with user authorities
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    // Step 9: Attach HTTP request details (IP address, Session ID if any) to auth token
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Step 10: Inject authenticated token into Spring SecurityContextHolder
                    // From this point onward, Spring Security treats the current HTTP request thread as fully authenticated
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token parsing or validation failed (e.g. expired or tampered token). Log and continue chain (will result in 401 Unauthorized)
            logger.error("Cannot set user authentication: {}", e);
        }

        // Step 11: Pass request along to next filter in chain (e.g., UsernamePasswordAuthenticationFilter or target Controller)
        filterChain.doFilter(request, response);
    }
}
