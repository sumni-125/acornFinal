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
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;

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
                logger.debug("응답이 커밋됐습니다. " + targetUrl + "로 리다이렉트할 수 없습니다.");
                return;
            }

            clearAuthenticationAttributes(request);

            // 세션 쿠키 명시적 설정
            setSessionCookie(request, response);

            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } catch (Exception e) {
            log.error("OAuth2 인증 성공 처리 중 오류 발생", e);
            throw new ServletException("인증 성공 처리 실패", e);
        }
    }

    private void setSessionCookie(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            log.warn("세션이 없어서 세션 쿠키를 설정할 수 없습니다.");
            return;
        }

        String sessionId = session.getId();
        log.debug("세션 쿠키 설정 시작 - 세션 ID: {}", sessionId);

        // 기존 세션 쿠키 확인
        boolean hasSessionCookie = false;
        if (request.getCookies() != null) {
            hasSessionCookie = Arrays.stream(request.getCookies())
                    .anyMatch(cookie -> "JSESSIONID".equals(cookie.getName()));
        }

        // 개발 환경과 프로덕션 환경 구분
        String serverName = request.getServerName();
        boolean isProduction = serverName != null && !serverName.contains("localhost");

        // 세션 쿠키 헤더 직접 설정
        response.addHeader("Set-Cookie",
                String.format("JSESSIONID=%s; Path=/; HttpOnly; SameSite=%s%s",
                        sessionId,
                        isProduction ? "None" : "Lax",
                        isProduction ? "; Secure" : ""));

        log.debug("세션 쿠키 설정 완료 - 세션 ID: {}, 기존 쿠키 있음: {}", sessionId, hasSessionCookie);
    }

    protected String determineTargetUrl(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) {
        try {
            log.info("토큰 생성 시작");
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            // userId를 사용하여 토큰 생성 (이전: getUsername() → getId())
            TokenResponse tokenResponse = tokenService.createTokens(userPrincipal.getId());

            // 리프레시 토큰을 HttpOnly 쿠키로 설정
            setRefreshTokenCookie(request, response, tokenResponse.getRefreshToken());

            String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                    .path("/oauth2/redirect")
                    .queryParam("token", tokenResponse.getAccessToken())
                    .build().toUriString();

            log.info("최종 리다이렉트 URL 생성 완료: {}", targetUrl);
            return targetUrl;
        } catch (Exception e) {
            log.error("토큰 생성 중 오류 발생", e);
            // 에러 페이지로 리다이렉트
            return UriComponentsBuilder.fromUriString(frontendUrl)
                    .path("/oauth2/redirect")
                    .queryParam("error", "token_creation_failed")
                    .queryParam("message", e.getMessage())
                    .build().toUriString();
        }
    }

    private void setRefreshTokenCookie(HttpServletRequest request, HttpServletResponse response, String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("리프레시 토큰이 null이거나 비어있어 쿠키를 설정할 수 없습니다.");
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
                isProduction ? "None" : "Lax",
                isProduction ? "; Secure" : ""
        );

        response.addHeader("Set-Cookie", cookieHeader);
        log.info("리프레시 토큰 쿠키 설정 완료 - 프로덕션: {}", isProduction);
    }
}