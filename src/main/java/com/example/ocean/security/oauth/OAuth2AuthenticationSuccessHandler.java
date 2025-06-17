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
            
            String targetUrl = determineTargetUrl(request, response, authentication);
            log.info("리다이렉트 URL: {}", targetUrl);

            if (response.isCommitted()) {
                logger.debug("응답이 커밋 됐습니다." + targetUrl + " 리다이렉트 할 수 없습니다.");
                return;
            }

            clearAuthenticationAttributes(request);
            
            // 세션 ID를 쿠키로 설정
            addSessionCookie(request, response, session);
            
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } catch (Exception e) {
            log.error("OAuth2 인증 성공 처리 중 오류 발생", e);
            throw new ServletException("OAuth2 인증 성공 처리 중 오류", e);
        }
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

    protected String determineTargetUrl(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) {
        try {
            log.info("토큰 생성 시작");
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            // 토큰 서비스를 통해 액세스 토큰과 리프레시 토큰 생성
            TokenResponse tokenResponse = tokenService.createTokens(userPrincipal.getUsername());

            
            // 리프레시 토큰을 HttpOnly 쿠키로 설정
            setRefreshTokenCookie(request, response, tokenResponse.getRefreshToken());

            String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                    .path("/oauth2/redirect")
                    .queryParam("token",tokenResponse.getAccessToken()) //엑세스 토큰 추가
                    .build().toUriString();
                    
            log.info("최종 리다이렉트 URL 생성 완료: {}", targetUrl);
            return targetUrl;
        } catch (Exception e) {
            log.error("토큰 생성 중 오류 발생", e);
            // 임시로 에러 페이지로 리다이렉트
            return UriComponentsBuilder.fromUriString(frontendUrl)
                    .path("/oauth2/redirect")
                    .queryParam("error", "token_creation_failed")
                    .queryParam("message", e.getMessage())
                    .build().toUriString();
        }
    }

    private void setRefreshTokenCookie(HttpServletRequest request, HttpServletResponse response, String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("리프레시 토큰이 null이거나 비어 있어 쿠키를 설정할 수 없습니다.");
            return;
        }

        log.debug("리프레시 토큰 쿠키 설정 시작 - 토큰 길이: {}", refreshToken.length());

        // 개발 환경과 프로덕션 환경 구분
        String serverName = request.getServerName();
        boolean isProduction = serverName != null && !serverName.contains("localhost");

        // 쿠키 헤더 직접 설정 (SameSite 속성 포함)
        String cookieHeader = String.format(
                "refreshToken=%s; Path=/; Max-Age=%d; HttpOnly; SameSite=%s%s",
                refreshToken,
                refreshTokenValidityInMs / 1000,
                isProduction ? "None" : "Lax",  // 로컬에서는 Lax, 프로덕션에서는 None
                isProduction ? "; Secure" : ""   // 프로덕션에서만 Secure
        );

        response.addHeader("Set-Cookie", cookieHeader);
        log.info("리프레시 토큰 쿠키 설정 완료 - 프로덕션: {}", isProduction);
    }
}
