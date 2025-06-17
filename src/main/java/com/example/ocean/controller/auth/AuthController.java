package com.example.ocean.controller.auth;

import com.example.ocean.dto.request.TokenRefreshRequest;
import com.example.ocean.dto.response.MessageResponse;
import com.example.ocean.dto.response.TokenResponse;
import com.example.ocean.dto.response.UserInfoResponse;
import com.example.ocean.security.oauth.UserPrincipal;
import com.example.ocean.security.jwt.JwtTokenProvider;
import com.example.ocean.service.TokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final TokenService tokenService;
    private final JwtTokenProvider jwtTokenProvider;
    
    @Value("${app.jwt.refresh-expiration}")
    private int refreshTokenValidityInMs;

    //OAuth2 로그인 엔드포인트 (프론트엔드에서 호출)
    @GetMapping("/oauth2/{provider}")
    public void redirectToAuth2Provider(@PathVariable String provider) {
        // SecurityConfig 에서 설정 한 경로(/oauth2/authorize/{provider}로 자동 리다이렉트
        // 시큐리티가 처리 하므로 메서드 안 내용 구현 안해도 됨
    }

    // 로그인 성공 후 사용자 정보 조회
    @GetMapping("/me")
    public UserInfoResponse getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {

            return UserInfoResponse.builder()
                    .userCode(userPrincipal.getId())
                    .email(userPrincipal.getEmail())
                    .build();
    }

    /**
     * 리프레시 토큰을 쿠키로 설정하는 API
     */
    @PostMapping("/set-refresh-token")
    public ResponseEntity<MessageResponse> setRefreshTokenCookie(
            @RequestBody TokenRefreshRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response) {
        
        log.debug("리프레시 토큰 쿠키 설정 요청 - 토큰 길이: {}", 
                  request.getRefreshToken() != null ? request.getRefreshToken().length() : 0);
        
        // 쿠키 설정
        Cookie cookie = new Cookie("refreshToken", request.getRefreshToken());
        cookie.setHttpOnly(true); // JavaScript에서 접근 불가능
        cookie.setPath("/");      // 전체 애플리케이션에서 접근 가능
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7일 (초 단위)
        
        // 프로덕션 환경에서는 Secure 속성 활성화
        String serverName = servletRequest.getServerName();
        if (serverName != null && !serverName.contains("localhost")) {
            cookie.setSecure(true);
        }
        
        // SameSite 속성 설정 (최신 브라우저에서 지원)
        String cookieHeader = String.format("%s=%s; Path=%s; Max-Age=%d; HttpOnly; SameSite=None; %s", 
                               cookie.getName(), 
                               cookie.getValue(), 
                               cookie.getPath(),
                               cookie.getMaxAge(),
                               cookie.getSecure() ? "Secure" : "");
        
        response.addHeader("Set-Cookie", cookieHeader);
        
        log.debug("리프레시 토큰 쿠키 설정 완료");
        
        return ResponseEntity.ok(new MessageResponse("리프레시 토큰이 쿠키에 성공적으로 저장되었습니다."));
    }
    
    /**
     * 액세스 토큰 갱신 API
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        log.debug("토큰 갱신 요청");
        
        // 쿠키에서 리프레시 토큰 추출
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }
        
        if (refreshToken == null) {
            log.warn("리프레시 토큰이 쿠키에 없음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("리프레시 토큰이 없습니다."));
        }
        
        try {
            // 리프레시 토큰으로 새 액세스 토큰 발급
            String newAccessToken = jwtTokenProvider.refreshToken(refreshToken);
            
            if (newAccessToken == null) {
                log.warn("액세스 토큰 갱신 실패");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new MessageResponse("리프레시 토큰이 유효하지 않습니다."));
            }
            
            log.debug("액세스 토큰 갱신 성공");
            TokenResponse tokenResponse = new TokenResponse();
            tokenResponse.setAccessToken(newAccessToken);
            return ResponseEntity.ok(tokenResponse);
        } catch (Exception e) {
            log.error("토큰 갱신 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("토큰 갱신 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 로그아웃 API - 리프레시 토큰 쿠키 삭제
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(HttpServletResponse response) {
        log.debug("로그아웃 요청");
        
        // 리프레시 토큰 쿠키 삭제
        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // 즉시 만료
        
        // SameSite 속성 설정
        String cookieHeader = String.format("%s=%s; Path=%s; Max-Age=%d; HttpOnly; SameSite=None; Secure", 
                               cookie.getName(), 
                               cookie.getValue(), 
                               cookie.getPath(),
                               cookie.getMaxAge());
        
        response.addHeader("Set-Cookie", cookieHeader);
        
        log.debug("리프레시 토큰 쿠키 삭제 완료");
        
        return ResponseEntity.ok(new MessageResponse("로그아웃 되었습니다."));
    }
}
