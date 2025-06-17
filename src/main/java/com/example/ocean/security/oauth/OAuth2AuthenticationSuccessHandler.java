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
                log.warn("세션이 없습니다!");
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
            
            // JSESSIONID 쿠키 설정 강화
            Cookie sessionCookie = new Cookie("JSESSIONID", session != null ? session.getId() : "");
            sessionCookie.setPath("/");
            sessionCookie.setHttpOnly(true);
            sessionCookie.setSecure(true);
            sessionCookie.setAttribute("SameSite", "None");
            response.addCookie(sessionCookie);
            
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } catch (Exception e) {
            log.error("OAuth2 인증 성공 처리 중 오류 발생", e);
            throw new ServletException("OAuth2 인증 성공 처리 중 오류", e);
        }
    }

    protected String determineTargetUrl(HttpServletRequest request,
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

            String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                    .path("/")
                    .queryParam("token", tokenResponse.getAccessToken())
                    .queryParam("refreshToken", tokenResponse.getRefreshToken())
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
}
