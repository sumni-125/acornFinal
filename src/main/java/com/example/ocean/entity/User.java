package com.example.ocean.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    // @Id : 해당 엔티티의 기본키 지정
    @Id
    @Column(name = "user_code")
    private String userCode;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "email")
    private String email;

    @Column(name = "user_profile_img")
    private String userProfileImg;

    @Column(name = "provider")
    private String provider;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "department")
    private String department;

    @Column(name = "position")
    private String position;

    @Builder.Default
    @Column(name = "is_profile_complete")
    private Boolean isProfileComplete = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "update_at")
    private LocalDateTime updateAt;
}
