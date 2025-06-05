package com.example.ocean.repository;

import com.example.ocean.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findbyEmail(String email);
    Optional<User> finbyUserCode(String userCode);
    Boolean existsByEmail(String email);
}
