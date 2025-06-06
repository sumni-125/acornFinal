package com.example.ocean.controller.user;


import com.example.ocean.dto.request.UserOnboardingRequest;
import com.example.ocean.dto.request.UserProfileUpdateRequest;
import com.example.ocean.dto.response.UserProfileResponse;
import com.example.ocean.security.oauth.UserPrincipal;
import com.example.ocean.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;


    //내 정보 조회
    @GetMapping("/profile")
    public UserProfileResponse getProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return userService.getUserProfile(userPrincipal.getId());
    }

    // 프로필 수정
    @PutMapping("/profile")
    public UserProfileResponse updateProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody UserProfileUpdateRequest request) {
        return userService.getUserProfile(userPrincipal.getId());
    }

    // 추가 정보 입력 (온보딩)
    @PostMapping("/onboarding")
    public UserProfileResponse compeleteOnboarding(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody UserOnboardingRequest request) {

        return userService.completeOnboarding(userPrincipal.getId(), request);
    }
}
