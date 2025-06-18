package com.example.ocean.controller.user;

import com.example.ocean.dto.request.UserOnboardingRequest;
import com.example.ocean.dto.request.UserProfileUpdateRequest;
import com.example.ocean.dto.response.UserProfileResponse;
import com.example.ocean.entity.WorkspaceMember;
import com.example.ocean.security.oauth.UserPrincipal;
import com.example.ocean.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    // 내 정보 조회 (첫 번째 워크스페이스 기준)
    @GetMapping("/profile")
    public UserProfileResponse getProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return userService.getUserProfile(userPrincipal.getId());
    }

    // 특정 워크스페이스에서의 내 정보 조회
    @GetMapping("/profile/{workspaceCd}")
    public UserProfileResponse getProfileInWorkspace(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String workspaceCd) {
        // TODO: 워크스페이스별 프로필 조회 구현
        return userService.getUserProfile(userPrincipal.getId());
    }

    // 프로필 수정 (워크스페이스별)
    @PutMapping("/profile/{workspaceCd}")
    public UserProfileResponse updateProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String workspaceCd,
            @RequestBody UserProfileUpdateRequest request) {
        return userService.updateUserProfile(userPrincipal.getId(), workspaceCd, request);
    }

    // 온보딩 (워크스페이스별)
    @PostMapping("/onboarding/{workspaceCd}")
    public UserProfileResponse completeOnboarding(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String workspaceCd,
            @RequestBody UserOnboardingRequest request) {
        return userService.completeOnboarding(userPrincipal.getId(), workspaceCd, request);
    }

    // 내가 속한 모든 워크스페이스 조회
    @GetMapping("/workspaces")
    public List<WorkspaceMember> getMyWorkspaces(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return userService.getUserWorkspaces(userPrincipal.getId());
    }
}