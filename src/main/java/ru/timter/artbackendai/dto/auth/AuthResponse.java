package ru.timter.artbackendai.dto.auth;

public record AuthResponse(
        String token,
        String username
) {
}
