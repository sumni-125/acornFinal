package com.example.ocean.controller.auth;

import com.example.ocean.dto.request.TokenRefreshRequest;
import com.example.ocean.dto.response.MessageResponse;
import com.example.ocean.dto.response.TokenResponse;
import com.example.ocean.dto.response.UserInfoResponse;
import com.example.ocean.security.oauth.UserPrincipal;
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
     * 리프레시 토큰으로 새 액세스 토큰을 발급합니다.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody TokenRefreshRequest request) {
        try {
            String refreshToken = request.getRefreshToken();
            TokenResponse tokenResponse = tokenService.refreshTokens(refreshToken);
            
            return ResponseEntity.ok(tokenResponse);
        } catch (Exception e) {
            log.error("토큰 갱신 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("토큰 갱신 실패: " + e.getMessage()));
        }
    }
    
    /**
     * 리프레시 토큰을 HttpOnly 쿠키로 설정합니다.
     */
    @PostMapping("/set-refresh-token")
    public ResponseEntity<?> setRefreshTokenCookie(@RequestBody Map<String, String> requestBody, 
                                                   HttpServletRequest request,
                                                   HttpServletResponse response) {
        try {
            String refreshToken = requestBody.get("refreshToken");
            
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest().body(new MessageResponse("리프레시 토큰이 필요합니다."));
            }
            
            // 리프레시 토큰을 HttpOnly 쿠키로 설정
            Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
            refreshTokenCookie.setHttpOnly(true); // JavaScript에서 접근 불가능
            refreshTokenCookie.setPath("/");      // 전체 애플리케이션에서 접근 가능
            
            // HTTPS 환경에서는 Secure 속성 활성화
            String serverName = request.getServerName();
            if (serverName != null && !serverName.contains("localhost")) {
                refreshTokenCookie.setSecure(true);
            }
            
            // 쿠키 만료 시간 설정 (리프레시 토큰과 동일하게)
            refreshTokenCookie.setMaxAge(refreshTokenValidityInMs / 1000); // 초 단위로 변환
            
            // SameSite 속성 설정 (최신 브라우저에서 지원)
            String sameSiteAttribute = "None";
            String cookieHeader = String.format("%s=%s; Path=%s; Max-Age=%d; HttpOnly; SameSite=%s%s", 
                                               refreshTokenCookie.getName(), 
                                               refreshTokenCookie.getValue(),
                                               refreshTokenCookie.getPath(),
                                               refreshTokenCookie.getMaxAge(),
                                               sameSiteAttribute,
                                               refreshTokenCookie.getSecure() ? "; Secure" : "");
            
            response.setHeader("Set-Cookie", cookieHeader);
            
            log.debug("리프레시 토큰 쿠키 설정 완료");
            return ResponseEntity.ok(new MessageResponse("리프레시 토큰이 쿠키에 안전하게 저장되었습니다."));
        } catch (Exception e) {
            log.error("리프레시 토큰 쿠키 설정 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("리프레시 토큰 쿠키 설정 실패: " + e.getMessage()));
        }
    }
    
    /**
     * 리프레시 토큰 쿠키에서 리프레시 토큰을 가져와 새 액세스 토큰을 발급합니다.
     */
    @PostMapping("/refresh-from-cookie")
    public ResponseEntity<?> refreshTokenFromCookie(HttpServletRequest request) {
        try {
            // 쿠키에서 리프레시 토큰 가져오기
            Cookie[] cookies = request.getCookies();
            String refreshToken = null;
            
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("refreshToken".equals(cookie.getName())) {
                        refreshToken = cookie.getValue();
                        break;
                    }
                }
            }
            
            if (refreshToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new MessageResponse("리프레시 토큰 쿠키가 없습니다."));
            }
            
            // 토큰 갱신
            TokenResponse tokenResponse = tokenService.refreshTokens(refreshToken);
            
            return ResponseEntity.ok(tokenResponse);
        } catch (Exception e) {
            log.error("쿠키 기반 토큰 갱신 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("토큰 갱신 실패: " + e.getMessage()));
        }
    }
    
    /**
     * 로그아웃 시 리프레시 토큰 쿠키를 삭제합니다.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        try {
            // 리프레시 토큰 쿠키 삭제
            Cookie refreshTokenCookie = new Cookie("refreshToken", "");
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setPath("/");
            refreshTokenCookie.setMaxAge(0); // 즉시 만료
            
            response.addCookie(refreshTokenCookie);
            
            return ResponseEntity.ok(new MessageResponse("로그아웃 되었습니다."));
        } catch (Exception e) {
            log.error("로그아웃 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("로그아웃 실패: " + e.getMessage()));
        }
    }
}
