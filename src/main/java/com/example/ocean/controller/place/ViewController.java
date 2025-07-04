package com.example.ocean.controller.place;

import com.example.ocean.dto.response.UserProfileResponse;
import com.example.ocean.security.oauth.UserPrincipal;
import com.example.ocean.service.PlaceService;
import com.example.ocean.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestParam;

@RequiredArgsConstructor
@Controller //뷰 반환 컨트롤러
public class ViewController {

    private final PlaceService service;
    private final UserService userService;

    @GetMapping("/meeting-place") // "/places" 경로로 GET 요청 처리 메소드
    public String getPlacesPage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) String workspaceCd,
            Model model
    ) {
        UserProfileResponse currentUser = userService.getUserProfile(userPrincipal.getId());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("workspaceCd", workspaceCd);
        return "meeting-place";
    }
}