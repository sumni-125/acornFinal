package com.example.ocean.controller.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;
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
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            HttpServletRequest request) {
        
        log.info("OAuth2 콜백 수신 - Provider: {}", provider);
        log.info("요청 URL: {}", request.getRequestURL());
        log.info("쿼리 스트링: {}", request.getQueryString());
        
        if (error != null) {
            log.error("OAuth2 콜백 에러 - Provider: {}", provider);
            log.error("에러 코드: {}", error);
            log.error("에러 설명: {}", errorDescription);
            
            // 에러 페이지로 리다이렉트
            return "redirect:/oauth2/redirect?error=" + error + 
                   "&error_description=" + (errorDescription != null ? errorDescription : "");
        }
        
        if (code != null) {
            log.info("인가 코드 수신 성공 - Provider: {}", provider);
            log.info("State: {}", state);
            // Spring Security가 이 코드를 사용하여 액세스 토큰을 요청합니다
        }
        
        // Spring Security의 기본 처리 흐름으로 계속 진행
        return null;
    }
}