package com.example.ocean.repository;

import com.example.ocean.entity.UserTokens;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserTokensRepository extends JpaRepository<UserTokens, String> {
    Optional<UserTokens> findByRefreshToken(String refreshToken);

    List<UserTokens> findByTokenExpiresAtBefore(LocalDateTime now);
}