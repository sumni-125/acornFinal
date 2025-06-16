package com.example.ocean.controller.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/oauth2")
public class OAuth2RedirectController {

    /**
     * OAuth2 인증 후 리다이렉트 페이지를 처리합니다.
     * 이 페이지는 인증 성공 후 토큰을 받아 로컬 스토리지에 저장하고
     * 메인 페이지로 리다이렉트합니다.
     * 
     * @return OAuth2 리다이렉트 페이지 템플릿
     */
    @GetMapping("/redirect")
    public String handleRedirect() {
        return "oauth2-redirect";
    }
} 