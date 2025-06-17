package com.example.ocean.security.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;

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
        
        // 요청 헤더 정보 로깅
        log.debug("요청 헤더 정보:");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            log.debug("{}: {}", headerName, request.getHeader(headerName));
        }
        
        // 쿠키 정보 로깅
        if (request.getCookies() != null) {
            log.debug("요청 쿠키: {}", Arrays.toString(request.getCookies()));
        } else {
            log.warn("요청에 쿠키가 없습니다!");
        }
        
        // 세션에서 저장된 인증 요청 정보 확인
        if (request.getSession(false) != null) {
            HttpSession session = request.getSession();
            log.error("세션 ID: {}", session.getId());
            log.debug("세션 속성들:");
            Enumeration<String> attributeNames = session.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String attributeName = attributeNames.nextElement();
                log.debug("{}: {}", attributeName, session.getAttribute(attributeName));
            }
        } else {
            log.error("세션이 없습니다! 이것이 문제의 원인일 수 있습니다.");
        }
        
        // 전체 스택 트레이스 로깅
        log.error("상세 에러:", exception);
        
        String errorMessage = exception.getMessage();
        if (errorMessage == null || errorMessage.isEmpty()) {
            errorMessage = "OAuth2 인증 중 오류가 발생했습니다.";
        }
        
        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                .queryParam("error", errorMessage.trim())
                .queryParam("error_type", exception.getClass().getSimpleName())
                .build().encode()
                .toUriString();
        
        log.debug("리다이렉트 URL: {}", targetUrl);

        // 새 세션 생성 시도
        HttpSession newSession = request.getSession(true);
        log.debug("새 세션 생성됨: {}", newSession.getId());
        
        // JSESSIONID 쿠키 설정 강화
        Cookie sessionCookie = new Cookie("JSESSIONID", newSession.getId());
        sessionCookie.setPath("/");
        sessionCookie.setHttpOnly(true);
        sessionCookie.setSecure(true);
        sessionCookie.setAttribute("SameSite", "None");
        response.addCookie(sessionCookie);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}