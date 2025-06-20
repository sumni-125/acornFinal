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
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
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
            log.error("=== OAuth2 인증 성공 핸들러 실행 시작 ===");
            log.error("Authentication Principal 타입: {}", authentication.getPrincipal().getClass().getName());
            log.error("Response isCommitted: {}", response.isCommitted());

            String targetUrl = determineTargetUrl(request, response, authentication);
            log.error("=== 최종 리다이렉트 URL: {} ===", targetUrl);

            if (response.isCommitted()) {
                logger.debug("응답이 커밋됐습니다. " + targetUrl + "로 리다이렉트할 수 없습니다.");
                return;
            }

            clearAuthenticationAttributes(request);
            setSessionCookie(request, response);
            response.sendRedirect(targetUrl);

        } catch (Exception e) {
            log.error("OAuth2 인증 성공 처리 중 오류 발생", e);
            throw new ServletException("인증 성공 처리 실패", e);
        }
    }

    protected String determineTargetUrl(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) {
        try {
            log.info("토큰 생성 시작");
            log.info("Authentication 객체 정보: {}", authentication);
            log.info("Principal 타입: {}", authentication.getPrincipal().getClass().getName());

            String userId = null;

            // Principal 타입에 따라 userId 추출
            if (authentication.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                userId = userPrincipal.getId();
                log.info("UserPrincipal에서 추출한 userId: {}", userId);
            } else if (authentication.getPrincipal() instanceof DefaultOAuth2User) {
                // CustomOAuth2UserService에서 UserPrincipal로 변환되지 않은 경우
                DefaultOAuth2User oauth2User = (DefaultOAuth2User) authentication.getPrincipal();

                // 카카오의 경우 id는 Long 타입으로 제공됨
                Object idObj = oauth2User.getAttribute("id");
                if (idObj != null) {
                    userId = "KAKAO_" + idObj.toString();
                    log.info("DefaultOAuth2User에서 추출한 userId: {}", userId);
                }

                log.info("OAuth2User attributes: {}", oauth2User.getAttributes());
            }

            if (userId == null) {
                log.error("userId를 추출할 수 없습니다!");
                throw new RuntimeException("사용자 ID를 찾을 수 없습니다");
            }

            // 토큰 생성
            TokenResponse tokenResponse = tokenService.createTokens(userId);
            log.info("토큰 생성 완료 - AccessToken: {}", tokenResponse.getAccessToken() != null ? "생성됨" : "null");

            // 리프레시 토큰을 HttpOnly 쿠키로 설정
            setRefreshTokenCookie(request, response, tokenResponse.getRefreshToken());

            String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                    .path("/oauth2/redirect")
                    .queryParam("token", tokenResponse.getAccessToken())
                    .build().toUriString();

            log.info("최종 리다이렉트 URL 생성 완료: {}", targetUrl);
            return targetUrl;

        } catch (Exception e) {
            log.error("토큰 생성 중 오류 발생: ", e);
            e.printStackTrace();

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

        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(refreshTokenValidityInMs / 1000);

        String serverName = request.getServerName();
        if (serverName != null && !serverName.contains("localhost")) {
            cookie.setSecure(true);
        }

        response.addCookie(cookie);
        log.debug("리프레시 토큰 쿠키 설정 완료");
    }

    private void setSessionCookie(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            log.warn("세션이 없어서 세션 쿠키를 설정할 수 없습니다.");
            return;
        }

        String sessionId = session.getId();
        log.debug("세션 쿠키 설정 시작 - 세션 ID: {}", sessionId);

        boolean hasSessionCookie = false;
        if (request.getCookies() != null) {
            hasSessionCookie = Arrays.stream(request.getCookies())
                    .anyMatch(cookie -> "JSESSIONID".equals(cookie.getName()));
        }

        String serverName = request.getServerName();
        boolean isProduction = serverName != null && !serverName.contains("localhost");

        response.addHeader("Set-Cookie",
                String.format("JSESSIONID=%s; Path=/; HttpOnly; SameSite=%s%s",
                        sessionId,
                        isProduction ? "None" : "Lax",
                        isProduction ? "; Secure" : ""));

        log.debug("세션 쿠키 설정 완료 - 세션 ID: {}, 기존 쿠키 있음: {}", sessionId, hasSessionCookie);
    }
}