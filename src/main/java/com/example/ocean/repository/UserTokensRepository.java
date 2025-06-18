package com.example.ocean.repository;

import com.example.ocean.entity.UserTokens;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserTokensRepository extends JpaRepository<UserTokens, String> {
    Optional<UserTokens> findByRefreshToken(String refreshToken);

    @Query("SELECT ut FROM UserTokens ut WHERE ut.user.userId = :userId")
    List<UserTokens> findByUserId(@Param("userId") String userId);

    List<UserTokens> findByTokenExpiresTimeBefore(LocalDateTime now);

    @Query("DELETE FROM UserTokens ut WHERE ut.user.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);
}