package ru.timter.artbackendai.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ru.timter.artbackendai.entity.User;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Security principal that also carries the user's UUID,
 * so controllers can resolve the real user id via @AuthenticationPrincipal.
 */
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(UUID id, String username, String password,
                         Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }

    public UUID getId() {
        return id;
    }

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
