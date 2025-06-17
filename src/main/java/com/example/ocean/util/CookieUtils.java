package com.example.ocean.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.SerializationUtils;

import java.util.Base64;
import java.util.Optional;

public class CookieUtils {

    /**
     * 요청에서 이름으로 쿠키를 찾습니다.
     */
    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }
        
        return Optional.empty();
    }

    /**
     * 응답에 쿠키를 추가합니다.
     */
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    /**
     * 쿠키를 삭제합니다. (만료시간을 0으로 설정)
     */
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                    break;
                }
            }
        }
    }

    /**
     * 객체를 직렬화하여 쿠키에 저장합니다.
     */
    public static void addSerializedCookie(HttpServletResponse response, String name, Object object, int maxAge) {
        String value = Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(object));
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    /**
     * SameSite=None 속성이 있는 쿠키를 추가합니다.
     */
    public static void addCookieWithSameSiteNone(HttpServletResponse response, String name, String value, int maxAge) {
        // 프로덕션 환경에서는 Secure 속성 추가
        String cookieHeader = String.format("%s=%s; Path=%s; Max-Age=%d; HttpOnly; SameSite=None; Secure", 
                             name, value, "/", maxAge);
        
        response.addHeader("Set-Cookie", cookieHeader);
    }
} 