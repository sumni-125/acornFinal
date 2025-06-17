package com.example.ocean.controller.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Map;

@Slf4j
@Controller
public class OAuth2CallbackController {

    /**
     * OAuth2 콜백 처리
     * Spring Security OAuth2가 자동으로 처리하지만, 로깅을 위해 추가
     */
    @GetMapping("/login/oauth2/code/{provider}")
    public String handleCallback(
            @PathVariable String provider,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response) {

        log.info("OAuth2 콜백 수신 - Provider: {}", provider);
        log.info("세션 ID: {}", request.getSession().getId());
        log.info("쿠키: {}", Arrays.toString(request.getCookies()));

        // 세션 쿠키가 없으면 생성
        if (request.getCookies() == null ||
                Arrays.stream(request.getCookies()).noneMatch(c -> "JSESSIONID".equals(c.getName()))) {

            Cookie sessionCookie = new Cookie("JSESSIONID", request.getSession().getId());
            sessionCookie.setPath("/");
            sessionCookie.setHttpOnly(true);
            sessionCookie.setSecure(true);
            sessionCookie.setDomain("ocean-app.click");
            response.addCookie(sessionCookie);

            log.info("세션 쿠키 생성: {}", sessionCookie.getValue());
        }

        // Spring Security가 처리하도록 null 반환
        return null;
    }
}