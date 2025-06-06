package com.example.ocean.service;

import com.example.ocean.dto.request.UserOnboardingRequest;
import com.example.ocean.dto.request.UserProfileUpdateRequest;
import com.example.ocean.dto.response.UserProfileResponse;
import com.example.ocean.entity.User;
import com.example.ocean.exception.ResourceNotFoundException;
import com.example.ocean.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional (readOnly = true)
public class UserService {

    private final UserRepository userRepository;


    // User entity를 User_Profile_Response DTO로 변환
    private UserProfileResponse mapToUserProfileResponse(User user) {
        return UserProfileResponse.builder()
                .userCode(user.getUserCode())
                .userId(user.getUserId())
                .userName(user.getUserName())
                .nickName(user.getNickname())
                .email(user.getEmail())
                .userProfileImg(user.getUserProfileImg())
                .provider(user.getProvider())
                .phoneNumber(user.getPhoneNumber())
                .department(user.getDepartment())
                .position(user.getPosition())
                .isProfileComplete(user.getIsProfileComplete())
                .isActive(user.getIsActive())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    //사용자 프로필 조회
    public UserProfileResponse getUserProfile(String userCode) {
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        return  mapToUserProfileResponse(user);
    }

    //프로필 수정 메서드
    @Transactional
    public UserProfileResponse updateUserProfile(String userCode, UserProfileUpdateRequest request) {
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        //프로필 정보 업데이트
        if (request.getNickName() != null) {
            user.setNickname(request.getNickName());
        }

        if (request.getPhonNumer() != null) {
            user.setPhoneNumber(request.getPhonNumer());
        }

        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment());
        }

        if (request.getPosition() != null) {
            user.setPosition(request.getPosition());
        }

        user.setUpdateAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        log.info(" 사용자 프로필 수정 완료 : {} " , userCode);
        return mapToUserProfileResponse(savedUser);
    }

    // 온보딩 완료
    @Transactional
    public UserProfileResponse completeOnboarding(String userCode, UserOnboardingRequest request) {
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        //온보딩 정보 설정
        user.setNickname(request.getNickname());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setDepartment(request.getDepartment());
        user.setPosition(request.getPositionl());
        user.setUpdateAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        log.info(" 사용자 온보딩 완료 : {} " ,userCode);
        return mapToUserProfileResponse(savedUser);
    }

}
