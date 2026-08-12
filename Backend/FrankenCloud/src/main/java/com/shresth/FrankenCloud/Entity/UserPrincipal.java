package com.shresth.FrankenCloud.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * WHAT: Custom Security Principal wrapping MongoDB User details and ObjectId.
 * HOW: Implements Spring Security's UserDetails interface while retaining user.getId().
 * WHY: Enables any Controller or Service to extract the authenticated user's ObjectId directly from SecurityContextHolder without querying MongoDB again or trusting frontend-sent user IDs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    /** MongoDB User ObjectId */
    private ObjectId id;

    /** Unique Username or Email */
    private String username;

    /** User Email */
    private String email;

    /** BCrypt hashed password */
    private String password;

    /** Granted authorities/roles */
    private Collection<? extends GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
