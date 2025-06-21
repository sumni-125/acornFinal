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
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            String userId = userPrincipal.getId();

            log.info("=== OAuth2 인증 성공 ===");
            log.info("UserPrincipal ID: {}", userId);

            // DB 트랜잭션 대기
            Thread.sleep(1000);

            // 토큰 생성
            TokenResponse tokenResponse = tokenService.createTokens(userId);
            log.info("토큰 생성 완료");

            // 리프레시 토큰 쿠키 설정
            addRefreshTokenCookie(response, tokenResponse.getRefreshToken());

            // 액세스 토큰도 임시 쿠키로 설정
            Cookie tempTokenCookie = new Cookie("tempAccessToken", tokenResponse.getAccessToken());
            tempTokenCookie.setPath("/");
            tempTokenCookie.setMaxAge(60); // 60초만 유효
            tempTokenCookie.setHttpOnly(false); // JavaScript에서 읽을 수 있도록

            // 환경에 따라 Secure 설정
            String serverName = request.getServerName();
            if (!serverName.equals("localhost")) {
                tempTokenCookie.setSecure(true);
            }

            response.addCookie(tempTokenCookie);
            log.info("임시 액세스 토큰 쿠키 설정 완료");

            // 토큰을 URL 파라미터로 전달하도록 수정
            String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                    .path("/oauth2-redirect.html")
                    .queryParam("token", tokenResponse.getAccessToken())
                    .build().toUriString();
            
            log.info("리다이렉트 URL: {}", redirectUrl);

            return redirectUrl;

        } catch (Exception e) {
            log.error("OAuth2 성공 핸들러 오류", e);
            return frontendUrl + "/login?error=authentication_failed";
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