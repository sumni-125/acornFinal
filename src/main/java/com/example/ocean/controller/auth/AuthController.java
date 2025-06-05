package com.example.ocean.controller.auth;

import com.example.ocean.dto.response.MessageResponse;
import com.example.ocean.dto.response.UserInfoResponse;
import com.example.ocean.security.oauth.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {

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

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Configsecurity에서 세션을 stateless로 줬으므로 처리 불필요
        // 프론트엔드에서 토큰을 삭제 하면 된다.
        return ResponseEntity.ok(new MessageResponse("로그아웃 됐습니다."));
    }
}
