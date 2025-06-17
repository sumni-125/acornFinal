package com.example.ocean.repository;

import com.example.ocean.entity.User;
import com.example.ocean.entity.UserTokens;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserTokensRepository extends JpaRepository<UserTokens, Long> {
    Optional<UserTokens> findByUser(User user);
    Optional<UserTokens> findByRefreshToken(String refreshToken);
    void deleteByUser(User user);
    List<UserTokens> findByExpiryDateBefore(LocalDateTime now);
}