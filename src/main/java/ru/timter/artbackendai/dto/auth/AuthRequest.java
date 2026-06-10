package ru.timter.artbackendai.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Used for both registration and login.
 */
public record AuthRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 6, max = 100) String password
) {
}
