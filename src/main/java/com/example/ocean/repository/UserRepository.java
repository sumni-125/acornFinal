package com.example.ocean.repository;

import com.example.ocean.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUserCode(String userCode);
    Optional<User> findByUserIdAndProvider(String userId, String provider);
    Boolean existsByEmail(String email);
}
