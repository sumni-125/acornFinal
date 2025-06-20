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
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
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
    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Value("${app.frontend.url:http://localhost:8080}")
    private String frontendUrl;

    @Value("${app.jwt.refresh-expiration}")
    private int refreshTokenValidityInMs;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // Response가 이미 커밋되었는지 확인
        if (response.isCommitted()) {
            log.debug("Response가 이미 커밋되어 리다이렉트할 수 없습니다.");
            return;
        }

        // 세션에서 리다이렉트 루프 카운터 확인
        HttpSession session = request.getSession();
        Integer redirectCount = (Integer) session.getAttribute("oauth2_redirect_count");
        if (redirectCount == null) {
            redirectCount = 0;
        } else if (redirectCount > 2) {
            // 무한 리다이렉트 방지
            log.error("OAuth2 리다이렉트 루프 감지됨");
            session.removeAttribute("oauth2_redirect_count");
            response.sendRedirect("/login?error=redirect_loop");
            return;
        }
        session.setAttribute("oauth2_redirect_count", redirectCount + 1);

        try {
            String targetUrl = determineTargetUrl(request, response, authentication);

            // 리다이렉트 성공 시 카운터 제거
            session.removeAttribute("oauth2_redirect_count");

            clearAuthenticationAttributes(request);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (Exception e) {
            log.error("OAuth2 인증 성공 처리 중 오류", e);
            session.removeAttribute("oauth2_redirect_count");
            response.sendRedirect("/login?error=authentication_failed");
        }
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) {

        // 저장된 요청 확인
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        String targetUrl = null;

        if (savedRequest != null) {
            targetUrl = savedRequest.getRedirectUrl();
            // OAuth2 관련 경로인 경우 무시
            if (isOAuth2RelatedPath(targetUrl)) {
                targetUrl = null;
            }
        }

        // 기본 URL 설정
        if (targetUrl == null || targetUrl.isEmpty()) {
            targetUrl = getDefaultTargetUrl();
        }

        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            // JWT 토큰 생성
            TokenResponse tokenResponse = tokenService.createTokens(userPrincipal.getId());

            // 리프레시 토큰을 HttpOnly 쿠키로 설정
            addRefreshTokenCookie(response, tokenResponse.getRefreshToken());

            // 액세스 토큰을 URL 파라미터로 전달
            return UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("token", tokenResponse.getAccessToken())
                    .build().toUriString();

        } catch (Exception e) {
            log.error("토큰 생성 중 오류", e);
            return UriComponentsBuilder.fromUriString("/login")
                    .queryParam("error", "token_generation_failed")
                    .build().toUriString();
        }
    }

    private boolean isOAuth2RelatedPath(String path) {
        if (path == null) return false;

        String[] oauth2Paths = {"/oauth2/", "/login/oauth2/", "/login", "/favicon"};
        return Arrays.stream(oauth2Paths).anyMatch(path::contains);
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // HTTPS에서만 전송
        cookie.setPath("/");
        cookie.setMaxAge(refreshTokenValidityInMs / 1000); // 초 단위로 변환
        cookie.setDomain("ocean-app.click");
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }
}