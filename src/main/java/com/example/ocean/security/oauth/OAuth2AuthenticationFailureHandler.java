package com.example.ocean.security.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
// TODO : 로그인 실패 시 처리하는 핸들러
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    
    // app.frontend.url 값이 없으면 http://localhost:8080 로 설정
    @Value("${app.frontend.url:http://localhost:8080}")
    private String frontendUrl;
    
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        // 에러 로깅 추가
        log.error("OAuth2 인증 실패");
        log.error("요청 URI: {}", request.getRequestURI());
        log.error("에러 메시지: {}", exception.getMessage());
        log.error("에러 타입: {}", exception.getClass().getName());
        
        // 세션에서 저장된 인증 요청 정보 확인
        if (request.getSession(false) != null) {
            log.error("세션 ID: {}", request.getSession().getId());
        }
        
        // 전체 스택 트레이스 로깅
        log.error("상세 에러:", exception);
        
        String errorMessage = exception.getMessage();
        if (errorMessage == null || errorMessage.isEmpty()) {
            errorMessage = "OAuth2 인증 중 오류가 발생했습니다.";
        }
        
        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                .queryParam("error", errorMessage)
                .queryParam("error_type", exception.getClass().getSimpleName())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request,response,targetUrl);
    }
}