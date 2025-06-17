package com.example.ocean.security.oauth;
import com.example.ocean.dto.response.TokenResponse;
import com.example.ocean.security.jwt.JwtTokenProvider;
import com.example.ocean.service.TokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
// TODO : 로그인 성공 시 처리하는 핸들러
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;

    //Spring @Value는 프로 퍼티 값을 주입 받을 떄 사용 함.
    @Value("${app.frontend.url:http://localhost:8080}")
    private String frontendUrl;
    
    @Value("${app.jwt.refresh-expiration}")
    private int refreshTokenValidityInMs;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        try {
            log.info("OAuth2 인증 성공 - Principal: {}", authentication.getPrincipal());
            
            // 세션 정보 로깅
            HttpSession session = request.getSession(false);
            if (session != null) {
                log.debug("세션 ID: {}, 생성 시간: {}, 마지막 접근 시간: {}", 
                          session.getId(), 
                          session.getCreationTime(), 
                          session.getLastAccessedTime());
            } else {
                log.warn("세션이 없습니다! 새 세션을 생성합니다.");
                session = request.getSession(true);
                log.debug("새 세션 생성됨 - ID: {}", session.getId());
            }
            
            // 쿠키 정보 로깅
            if (request.getCookies() != null) {
                log.debug("요청 쿠키: {}", Arrays.toString(request.getCookies()));
            } else {
                log.warn("요청에 쿠키가 없습니다!");
            }
            
            // 토큰 생성 및 리다이렉트 URL 생성
            TokenAndUrlResult result = determineTargetUrlAndCreateTokens(request, response, authentication);
            String targetUrl = result.getTargetUrl();
            
            log.info("리다이렉트 URL: {}", targetUrl);

            if (response.isCommitted()) {
                logger.debug("응답이 커밋 됐습니다." + targetUrl + " 리다이렉트 할 수 없습니다.");
                return;
            }

            clearAuthenticationAttributes(request);
            
            // 세션 ID를 쿠키로 설정
            addSessionCookie(request, response, session);
            
            // 리프레시 토큰을 HttpOnly 쿠키로 설정 (직접 설정)
            if (result.getRefreshToken() != null) {
                addRefreshTokenCookie(request, response, result.getRefreshToken());
            }
            
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } catch (Exception e) {
            log.error("OAuth2 인증 성공 처리 중 오류 발생", e);
            throw new ServletException("OAuth2 인증 성공 처리 중 오류", e);
        }
    }
    
    /**
     * 리프레시 토큰을 HttpOnly 쿠키로 설정하는 메서드
     */
    private void addRefreshTokenCookie(HttpServletRequest request, HttpServletResponse response, String refreshToken) {
        // 리프레시 토큰 쿠키 설정
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true); // JavaScript에서 접근 불가능
        refreshTokenCookie.setPath("/");      // 전체 애플리케이션에서 접근 가능
        
        // HTTPS 환경에서는 Secure 속성 활성화
        String serverName = request.getServerName();
        if (serverName != null && !serverName.contains("localhost")) {
            refreshTokenCookie.setSecure(true);
        }
        
        // 쿠키 만료 시간 설정 (리프레시 토큰과 동일하게)
        refreshTokenCookie.setMaxAge(refreshTokenValidityInMs / 1000); // 초 단위로 변환
        
        // SameSite 속성 설정 (최신 브라우저에서 지원)
        String sameSiteAttribute = "None";
        String cookieHeader = String.format("%s=%s; Path=%s; Max-Age=%d; HttpOnly; SameSite=%s%s", 
                               refreshTokenCookie.getName(), 
                               refreshTokenCookie.getValue(),
                               refreshTokenCookie.getPath(),
                               refreshTokenCookie.getMaxAge(),
                               sameSiteAttribute,
                               refreshTokenCookie.getSecure() ? "; Secure" : "");
        
        response.setHeader("Set-Cookie", cookieHeader);
        
        log.debug("리프레시 토큰 쿠키 설정 완료 - 만료 시간: {} 초", refreshTokenCookie.getMaxAge());
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
    
    /**
     * 토큰 생성 및 리다이렉트 URL을 생성하는 메서드
     */
    private static class TokenAndUrlResult {
        private final String targetUrl;
        private final String refreshToken;
        
        public TokenAndUrlResult(String targetUrl, String refreshToken) {
            this.targetUrl = targetUrl;
            this.refreshToken = refreshToken;
        }
        
        public String getTargetUrl() {
            return targetUrl;
        }
        
        public String getRefreshToken() {
            return refreshToken;
        }
    }

    protected TokenAndUrlResult determineTargetUrlAndCreateTokens(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) {
        try {
            log.info("토큰 생성 시작");
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            log.info("UserPrincipal - Username: {}, UserCode: {}", userPrincipal.getUsername(), userPrincipal.getUserCode());
            
            if (tokenService == null) {
                log.error("TokenService is null!");
                throw new IllegalStateException("TokenService is not initialized");
            }
            
            // 토큰 서비스를 통해 액세스 토큰과 리프레시 토큰 생성
            TokenResponse tokenResponse = tokenService.createTokens(userPrincipal.getUsername());
            
            if (tokenResponse == null) {
                log.error("TokenResponse is null!");
                throw new IllegalStateException("Failed to create tokens");
            }
            
            log.info("토큰 생성 완료 - AccessToken 길이: {}", tokenResponse.getAccessToken().length());

            // 리프레시 토큰은 쿠키로 설정하고, 액세스 토큰만 URL 파라미터로 전달
            String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                    .path("/")
                    .queryParam("token", tokenResponse.getAccessToken())
                    // 리프레시 토큰은 URL에 포함하지 않음
                    .build().toUriString();
                    
            log.info("최종 리다이렉트 URL 생성 완료: {}", targetUrl);
            return new TokenAndUrlResult(targetUrl, tokenResponse.getRefreshToken());
        } catch (Exception e) {
            log.error("토큰 생성 중 오류 발생", e);
            // 임시로 에러 페이지로 리다이렉트
            String errorUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                    .path("/oauth2/redirect")
                    .queryParam("error", "token_creation_failed")
                    .queryParam("message", e.getMessage())
                    .build().toUriString();
            return new TokenAndUrlResult(errorUrl, null);
        }
    }
}
