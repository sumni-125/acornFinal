package com.example.ocean.controller.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/oauth2")
public class OAuth2RedirectController {

    /**
     * OAuth2 인증 후 리다이렉트 페이지를 처리
     * 이 페이지는 인증 성공 후 토큰을 받아 로컬 스토리지에 저장하고
     * 메인 페이지로 리다이렉트
     * 
     * @return OAuth2 리다이렉트 페이지
     */
    /*
    @GetMapping("/redirect")
    public String handleRedirect(@RequestParam Map<String, String> params, HttpServletRequest request) {
        log.info("OAuth2 리다이렉트 처리");
        log.info("요청 URL: {}", request.getRequestURL());
        log.info("쿼리 파라미터: {}", params);
        
        if (params.containsKey("error")) {
            log.error("OAuth2 리다이렉트 에러: {}", params.get("error"));
        }
        
        return "oauth2-redirect";
    }
     */
} 