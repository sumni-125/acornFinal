package com.example.ocean.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserProfileResponse {
    private String userCode;
    private String userId;
    private String nickName;
    private String email;
    private String userProfileImg;
    private String provider;
    private String phoneNumber;
    private String department;
    private String position;
    private Boolean isProfileComplete;
    private Boolean isActive;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
