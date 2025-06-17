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
import java.util.UUID;

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
        HttpSession session = request.getSession(false);
        if (session != null) {
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
        
        // 에러 정보에 세션 상태 추가
        String errorDetail = errorMessage + " (세션 상태: " + (session != null ? "있음" : "없음") + ")";
        
        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                .queryParam("error", errorDetail.trim())
                .queryParam("error_type", exception.getClass().getSimpleName())
                .queryParam("recovery", "true") // 복구 시도 플래그 추가
                .queryParam("state", UUID.randomUUID().toString()) // 새로운 상태값 생성
                .build().encode()
                .toUriString();
        
        log.debug("리다이렉트 URL: {}", targetUrl);

        // 새 세션 생성 시도
        HttpSession newSession = request.getSession(true);
        log.debug("새 세션 생성됨: {}", newSession.getId());
        
        // JSESSIONID 쿠키 설정 강화
        addSessionCookie(request, response, newSession);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
    
    /**
     * 세션 ID를 쿠키로 설정하는 메서드
     */
    private void addSessionCookie(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        if (session == null) {
            log.warn("세션이 null이므로 세션 쿠키를 설정할 수 없습니다.");
            return;
        }
        
        String sessionId = session.getId();
        
        // 기존 JSESSIONID 쿠키 확인
        Cookie[] cookies = request.getCookies();
        boolean hasSessionCookie = false;
        
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("JSESSIONID".equals(cookie.getName())) {
                    hasSessionCookie = true;
                    log.debug("기존 JSESSIONID 쿠키 발견: {}", cookie.getValue());
                    break;
                }
            }
        }
        
        // 세션 쿠키 설정
        Cookie sessionCookie = new Cookie("JSESSIONID", sessionId);
        sessionCookie.setPath("/");
        sessionCookie.setHttpOnly(true);
        
        // 프로덕션 환경에서는 secure=true로 설정
        String serverName = request.getServerName();
        if (serverName != null && !serverName.contains("localhost")) {
            sessionCookie.setSecure(true);
            log.debug("프로덕션 환경에서 secure 쿠키 설정");
        }
        
        // SameSite 속성 설정 (최신 브라우저에서 지원)
        // 참고: Java 서블릿 API에서는 SameSite 속성을 직접 지원하지 않으므로
        // 헤더로 설정해야 함
        response.setHeader("Set-Cookie", String.format("%s=%s; Path=%s; HttpOnly; SameSite=None; %s", 
                           sessionCookie.getName(), 
                           sessionCookie.getValue(), 
                           sessionCookie.getPath(),
                           sessionCookie.getSecure() ? "Secure" : ""));
        
        log.debug("세션 쿠키 설정 완료 - 세션 ID: {}, 기존 쿠키 있음: {}", sessionId, hasSessionCookie);
    }
}