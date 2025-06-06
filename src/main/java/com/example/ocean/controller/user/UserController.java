package com.example.ocean.controller.user;


import com.example.ocean.dto.response.UserProfileResponse;
import com.example.ocean.security.oauth.UserPrincipal;
import com.example.ocean.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;


    //내 정보 조회
    @GetMapping("/profile")
    public UserProfileResponse getProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return userService.getUserProfile("")
    }
}
