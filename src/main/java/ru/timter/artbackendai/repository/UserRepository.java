package ru.timter.artbackendai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.timter.artbackendai.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
