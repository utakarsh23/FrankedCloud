package com.shresth.FrankenCloud.Services;

import com.shresth.FrankenCloud.Entity.User;
import com.shresth.FrankenCloud.Entity.UserPrincipal;
import com.shresth.FrankenCloud.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * WHAT: Custom implementation of Spring Security's UserDetailsService interface.
 * HOW: Queries MongoDB via UserRepository and constructs a UserPrincipal containing user ObjectId.
 * WHY: Attaches the user's database ObjectId to Spring Security context so controllers can extract userId directly.
 */
@Service
public class UserDetailServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    /**
     * @param identifier Username or Email string submitted by client or extracted from JWT token.
     * @return UserPrincipal populated with user ObjectId, credentials, and default roles.
     */
    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userRepository.findUserByEmailOrUsername(identifier, identifier);

        if (user == null) {
            user = userRepository.findUserByEmail(identifier);
        }
        if (user == null) {
            user = userRepository.findUserByUsername(identifier);
        }

        if (user == null) {
            throw new UsernameNotFoundException("User not found with email or username: " + identifier);
        }

        return new UserPrincipal(
                user.getId(),
                user.getUsername() != null ? user.getUsername() : user.getEmail(),
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
