package ru.timter.artbackendai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.timter.artbackendai.dto.auth.AuthResponse;
import ru.timter.artbackendai.entity.User;
import ru.timter.artbackendai.repository.UserRepository;
import ru.timter.artbackendai.security.JwtService;
import ru.timter.artbackendai.security.UserPrincipal;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(String username, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken");
        }
        User user = User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role("USER")
                .build();
        userRepository.save(user);
        log.info("Registered new user: {}", username);

        String token = jwtService.generateToken(UserPrincipal.from(user));
        return new AuthResponse(token, username);
    }

    public AuthResponse login(String username, String rawPassword) {
        // Throws AuthenticationException (-> 401) on bad credentials.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, rawPassword));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        String token = jwtService.generateToken(UserPrincipal.from(user));
        return new AuthResponse(token, username);
    }
}
