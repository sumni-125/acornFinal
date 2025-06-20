package com.example.ocean.controller.auth;

import com.example.ocean.dto.response.TokenResponse;
import com.example.ocean.entity.User;
import com.example.ocean.repository.UserRepository;
import com.example.ocean.security.oauth.provider.KakaoOAuth2UserInfo;
import com.example.ocean.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
public class OAuth2CallbackController {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${app.frontend.url:https://ocean-app.click}")
    private String frontendUrl;

    @Value("${app.jwt.refresh-expiration}")
    private int refreshTokenValidityInMs;

    @GetMapping("/login/oauth2/code/{registrationId}")
    public void handleCallback(
            @PathVariable String registrationId,
            @RequestParam String code,
            @RequestParam String state,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        log.info("=== OAuth2 콜백 수동 처리 시작 ===");
        log.info("Provider: {}, Code: {}", registrationId, code);

        try {
            if ("kakao".equalsIgnoreCase(registrationId)) {
                handleKakaoCallback(code, request, response);
            } else {
                log.error("지원하지 않는 OAuth Provider: {}", registrationId);
                response.sendRedirect(frontendUrl + "/login?error=unsupported_provider");
            }
        } catch (Exception e) {
            log.error("OAuth2 콜백 처리 중 오류", e);
            response.sendRedirect(frontendUrl + "/login?error=oauth2_callback_failed");
        }
    }

    private void handleKakaoCallback(String code, HttpServletRequest request,
                                     HttpServletResponse response) throws IOException {
        try {
            // 1. 카카오에서 액세스 토큰 받기
            String kakaoAccessToken = getKakaoAccessToken(code, request);
            log.info("카카오 액세스 토큰 획득 성공");

            // 2. 액세스 토큰으로 사용자 정보 받기
            Map<String, Object> userInfo = getKakaoUserInfo(kakaoAccessToken);
            log.info("카카오 사용자 정보 획득 성공");

            // 3. 사용자 정보로 회원가입/로그인 처리
            User user = processOAuth2User(userInfo);
            log.info("사용자 처리 완료: {}", user.getUserId());

            // 4. JWT 토큰 생성
            TokenResponse tokenResponse = tokenService.createTokens(user.getUserId());
            log.info("JWT 토큰 생성 완료");

            // 5. 쿠키 설정
            addRefreshTokenCookie(response, tokenResponse.getRefreshToken());
            addTempAccessTokenCookie(response, tokenResponse.getAccessToken());

            // 6. 메인 페이지로 리다이렉트
            response.sendRedirect(frontendUrl);

        } catch (Exception e) {
            log.error("카카오 OAuth 처리 중 오류", e);
            response.sendRedirect(frontendUrl + "/login?error=kakao_auth_failed");
        }
    }

    private String getKakaoAccessToken(String code, HttpServletRequest request) {
        String tokenUrl = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("client_secret", kakaoClientSecret);
        params.add("redirect_uri", getRedirectUri(request));
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                httpEntity,
                Map.class
        );

        Map<String, Object> responseBody = response.getBody();
        return (String) responseBody.get("access_token");
    }

    private Map<String, Object> getKakaoUserInfo(String accessToken) {
        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                userInfoUrl,
                HttpMethod.GET,
                httpEntity,
                Map.class
        );

        return response.getBody();
    }

    private User processOAuth2User(Map<String, Object> attributes) {
        KakaoOAuth2UserInfo kakaoUserInfo = new KakaoOAuth2UserInfo(attributes);

        String userId = "kakao_" + kakaoUserInfo.getId();
        String name = kakaoUserInfo.getName();
        String imageUrl = kakaoUserInfo.getImageUrl();

        Optional<User> existingUser = userRepository.findById(userId);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // 기존 사용자 정보 업데이트
            user.setUserName(name);
            user.setUserImg(imageUrl);
            return userRepository.save(user);
        } else {
            // 신규 사용자 생성
            User newUser = User.builder()
                    .userId(userId)
                    .userName(name)
                    .userImg(imageUrl)
                    .provider(User.Provider.valueOf("kakao"))
                    .activeState("Y")
                    .languageSetting("ko")
                    .build();
            return userRepository.save(newUser);
        }
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(refreshTokenValidityInMs / 1000);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private void addTempAccessTokenCookie(HttpServletResponse response, String accessToken) {
        Cookie cookie = new Cookie("tempAccessToken", accessToken);
        cookie.setPath("/");
        cookie.setMaxAge(60); // 60초
        cookie.setHttpOnly(false); // JavaScript에서 읽을 수 있도록
        cookie.setSecure(true);
        response.addCookie(cookie);
    }

    private String getRedirectUri(HttpServletRequest request) {
        // 실제 환경에 맞게 조정
        return frontendUrl + "/login/oauth2/code/kakao";
    }
}