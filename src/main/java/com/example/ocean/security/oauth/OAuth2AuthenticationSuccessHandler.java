package com.example.ocean.security.oauth;

import com.example.ocean.dto.response.TokenResponse;
import com.example.ocean.security.jwt.JwtTokenProvider;
import com.example.ocean.service.TokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;

    @Value("${app.frontend.url:https://ocean-app.click}")
    private String frontendUrl;

    @Value("${app.jwt.refresh-expiration}")
    private int refreshTokenValidityInMs;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        log.info("=== OAuth2 인증 성공 처리 시작 ===");

        // Response가 이미 커밋되었는지 확인
        if (response.isCommitted()) {
            log.warn("Response가 이미 커밋되어 리다이렉트할 수 없습니다.");
            return;
        }

        try {
            // 토큰 생성 및 리다이렉트 URL 구성
            String targetUrl = determineTargetUrl(request, response, authentication);

            log.info("최종 리다이렉트 URL: {}", targetUrl);

            // 인증 속성 정리
            clearAuthenticationAttributes(request);

            // 리다이렉트 실행
            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (Exception e) {
            log.error("OAuth2 인증 성공 처리 중 오류", e);
            response.sendRedirect(frontendUrl + "/login?error=authentication_failed");
        }
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) {

        try {
            // UserPrincipal 추출
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            String userId = userPrincipal.getId();

            log.info("=== OAuth2 인증 성공 ===");
            log.info("UserPrincipal ID: {}", userId);
            log.info("UserPrincipal Name: {}", userPrincipal.getName());
            log.info("UserPrincipal Attributes: {}", userPrincipal.getAttributes());

            // DB 트랜잭션이 커밋될 시간을 주기 위해 잠시 대기
            try {
                Thread.sleep(500); // 0.5초 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // JWT 토큰 생성 시도
            TokenResponse tokenResponse = null;
            int retryCount = 0;
            Exception lastException = null;

            // 최대 3번 시도
            while (retryCount < 3 && tokenResponse == null) {
                try {
                    tokenResponse = tokenService.createTokens(userId);
                    log.info("토큰 생성 성공 (시도 {}번)", retryCount + 1);
                    break;
                } catch (Exception e) {
                    lastException = e;
                    retryCount++;
                    log.warn("토큰 생성 실패 (시도 {}번): {}", retryCount, e.getMessage());

                    if (retryCount < 3) {
                        try {
                            Thread.sleep(1000); // 1초 대기
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }

            if (tokenResponse == null) {
                log.error("토큰 생성 최종 실패", lastException);
                throw new RuntimeException("토큰 생성 실패", lastException);
            }

            // 리프레시 토큰을 HttpOnly 쿠키로 설정
            addRefreshTokenCookie(response, tokenResponse.getRefreshToken());
            log.info("리프레시 토큰 쿠키 설정 완료");

            // OAuth2 리다이렉트 페이지로 이동 (토큰 포함)
            String redirectUrl = UriComponentsBuilder
                    .fromHttpUrl(frontendUrl)
                    .path("/oauth2/redirect")
                    .queryParam("token", tokenResponse.getAccessToken())
                    .build()
                    .toUriString();

            log.info("리다이렉트 URL 생성 완료: {}", redirectUrl);

            return redirectUrl;

        } catch (Exception e) {
            log.error("OAuth2 성공 핸들러에서 오류 발생", e);

            // 에러 시 로그인 페이지로 리다이렉트
            return UriComponentsBuilder
                    .fromHttpUrl(frontendUrl)
                    .path("/login")
                    .queryParam("error", "token_generation_failed")
                    .queryParam("message", e.getMessage())
                    .build()
                    .toUriString();
        }
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(refreshTokenValidityInMs / 1000);
        // cookie.setDomain() 호출하지 않음 - 현재 도메인 자동 사용
        cookie.setAttribute("SameSite", "Lax");

        response.addCookie(cookie);
    }
}