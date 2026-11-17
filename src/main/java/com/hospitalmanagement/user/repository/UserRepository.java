package com.hospitalmanagement.user.repository;

import com.hospitalmanagement.user.entity.User;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository
        extends
        JpaRepository<@NonNull User, @NonNull UUID>,
        JpaSpecificationExecutor<@NonNull User> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);
}
