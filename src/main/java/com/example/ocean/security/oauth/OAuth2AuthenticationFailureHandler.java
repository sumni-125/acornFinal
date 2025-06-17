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
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
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
        log.error("OAuth2 인증 실패: {}", exception.getMessage(), exception);
        
        // 세션 정보 로깅
        HttpSession session = request.getSession(false);
        if (session != null) {
            log.debug("세션 ID: {}, 생성 시간: {}, 마지막 접근 시간: {}", 
                      session.getId(), 
                      session.getCreationTime(), 
                      session.getLastAccessedTime());
        } else {
            log.warn("세션이 없습니다!");
        }
        
        // 쿠키 정보 로깅
        if (request.getCookies() != null) {
            log.debug("요청 쿠키: {}", Arrays.toString(request.getCookies()));
        } else {
            log.warn("요청에 쿠키가 없습니다!");
        }
        
        // 오류 정보 추출
        String errorCode = "unknown_error";
        String errorMessage = exception.getMessage();
        String errorType = exception.getClass().getSimpleName();
        boolean isAuthorizationRequestNotFound = false;
        
        if (exception instanceof OAuth2AuthenticationException) {
            OAuth2AuthenticationException oauth2Exception = (OAuth2AuthenticationException) exception;
            OAuth2Error oauth2Error = oauth2Exception.getError();
            errorCode = oauth2Error.getErrorCode();
            
            // authorization_request_not_found 오류 처리
            if ("authorization_request_not_found".equals(errorCode)) {
                log.warn("인증 요청을 찾을 수 없음 - 세션 문제일 수 있음");
                isAuthorizationRequestNotFound = true;
                
                // 세션 쿠키 설정 확인
                response.addHeader("Set-Cookie", "JSESSIONID=" + (session != null ? session.getId() : "new") + 
                                  "; Path=/; HttpOnly; SameSite=None; Secure");
            }
        }
        
        // 리다이렉트 URL 생성
        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/oauth2/redirect")
                .queryParam("error", errorCode)
                .queryParam("message", errorMessage)
                .queryParam("error_type", errorType)
                // 자동 복구 시도 파라미터 추가
                .queryParam("recovery", isAuthorizationRequestNotFound)
                .build().toUriString();
        
        log.debug("인증 실패 후 리다이렉트 URL: {}", targetUrl);
        
        // 리다이렉트 전에 응답 헤더 설정
        if (isAuthorizationRequestNotFound) {
            // SameSite=None 설정을 위한 쿠키 헤더 추가
            response.setHeader("Set-Cookie", "JSESSIONID=" + (session != null ? session.getId() : "new") + 
                              "; Path=/; HttpOnly; SameSite=None; Secure");
        }
        
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