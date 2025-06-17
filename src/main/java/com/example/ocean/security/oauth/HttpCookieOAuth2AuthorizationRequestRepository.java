package com.example.ocean.security.oauth;

import com.example.ocean.util.CookieUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.util.Base64;
import java.util.Optional;

@Slf4j
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final int COOKIE_EXPIRE_SECONDS = 180; // 3분

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        log.debug("OAuth2 인증 요청 로드 시도 - URI: {}", request.getRequestURI());
        
        // 모든 쿠키 로깅
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                log.debug("쿠키 발견: {} = {}", cookie.getName(), cookie.getName().equals(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME) ? "***값 생략***" : cookie.getValue());
            }
        } else {
            log.warn("요청에 쿠키가 없습니다!");
        }
        
        // 세션 정보 로깅
        if (request.getSession(false) != null) {
            log.debug("세션 ID: {}", request.getSession(false).getId());
        } else {
            log.warn("세션이 없습니다!");
        }
        
        return CookieUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> {
                    log.debug("OAuth2 인증 요청 쿠키를 찾았습니다. 역직렬화 시도");
                    return deserialize(cookie);
                })
                .orElseGet(() -> {
                    log.warn("OAuth2 인증 요청 쿠키를 찾을 수 없습니다!");
                    return null;
                });
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            log.debug("OAuth2 인증 요청이 null입니다. 관련 쿠키를 삭제합니다.");
            CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            CookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
            return;
        }

        log.debug("OAuth2 인증 요청을 쿠키에 저장합니다. URI: {}", request.getRequestURI());
        
        // 인증 요청을 쿠키에 저장
        addCookieWithSameSiteNone(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, serialize(authorizationRequest), COOKIE_EXPIRE_SECONDS);
        
        // 리다이렉트 URI가 있으면 함께 저장
        String redirectUriAfterLogin = request.getParameter("redirect_uri");
        if (redirectUriAfterLogin != null && !redirectUriAfterLogin.isEmpty()) {
            log.debug("리다이렉트 URI 쿠키 저장: {}", redirectUriAfterLogin);
            addCookieWithSameSiteNone(response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin, COOKIE_EXPIRE_SECONDS);
        }
        
        // 세션 정보 로깅
        if (request.getSession(false) != null) {
            log.debug("세션 ID: {}", request.getSession(false).getId());
        } else {
            log.debug("세션이 없어 새로 생성합니다.");
            request.getSession(true);
            log.debug("새 세션 ID: {}", request.getSession().getId());
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        log.debug("OAuth2 인증 요청 제거 시도 - URI: {}", request.getRequestURI());
        OAuth2AuthorizationRequest authRequest = loadAuthorizationRequest(request);
        if (authRequest != null) {
            log.debug("OAuth2 인증 요청을 찾았습니다. 쿠키를 삭제합니다.");
            CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        } else {
            log.warn("제거할 OAuth2 인증 요청을 찾을 수 없습니다!");
        }
        return authRequest;
    }

    /**
     * SameSite=None 속성이 포함된 쿠키를 추가하는 메서드
     */
    private void addCookieWithSameSiteNone(HttpServletResponse response, String name, String value, int maxAge) {
        // 기본 쿠키 생성
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        
        // 프로덕션 환경에서는 Secure 속성 추가
        String cookieHeader = String.format("%s=%s; Path=%s; Max-Age=%d; HttpOnly; SameSite=None; Secure", 
                               name, value, cookie.getPath(), maxAge);
        
        response.addHeader("Set-Cookie", cookieHeader);
        log.debug("쿠키 추가됨: {} (SameSite=None, Secure, HttpOnly)", name);
    }

    /**
     * 객체를 직렬화하여 Base64 문자열로 변환
     */
    private String serialize(Object object) {
        return Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(object));
    }

    /**
     * Base64 문자열을 역직렬화하여 객체로 변환
     */
    private OAuth2AuthorizationRequest deserialize(Cookie cookie) {
        try {
            return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(
                    Base64.getUrlDecoder().decode(cookie.getValue())
            );
        } catch (Exception e) {
            log.error("OAuth2 인증 요청 역직렬화 중 오류 발생", e);
            return null;
        }
    }
} 