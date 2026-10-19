package com.japaneselearning.user.repository;

import com.japaneselearning.user.entity.RefreshToken;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<@NonNull RefreshToken, @NonNull UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
