package com.japaneselearning.user.repository;

import com.japaneselearning.user.entity.User;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<@NonNull User, @NonNull UUID> {
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
