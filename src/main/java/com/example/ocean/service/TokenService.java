package com.example.ocean.service;

import com.example.ocean.dto.response.TokenResponse;
import com.example.ocean.entity.User;
import com.example.ocean.entity.UserTokens;
import com.example.ocean.exception.ResourceNotFoundException;
import com.example.ocean.repository.UserRepository;
import com.example.ocean.repository.UserTokensRepository;
import com.example.ocean.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserTokensRepository userTokensRepository;
    
    @Value("${app.jwt.expiration}")
    private int jwtExpirationInMs;

    @Transactional
    public TokenResponse createTokens(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        String accessToken = jwtTokenProvider.createToken(email);
        String refreshToken = jwtTokenProvider.createRefreshToken();

        // 토큰 만료 시간 계산
        Date expiryDate = jwtTokenProvider.getExpirationDateFromToken(refreshToken);
        LocalDateTime tokenExpiresAt = expiryDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        // 기존 토큰이 있으면 업데이트, 없으면 새로 생성
        UserTokens userTokens = userTokensRepository.findById(user.getUserCode())
                .orElse(UserTokens.builder()
                        .user(user)
                        .build());

        userTokens.setAccessToken(accessToken);
        userTokens.setRefreshToken(refreshToken);
        userTokens.setTokenExpiresAt(tokenExpiresAt);

        userTokensRepository.save(userTokens);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn((long) (jwtExpirationInMs / 1000))
                .build();
    }

    @Transactional
    // 리프레쉬 토큰 기능
    public TokenResponse refreshTokens(String refreshToken) {
        // 리프레시 토큰 유효성 검사
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 리프레시 토큰으로 사용자 찾기
        UserTokens userTokens = userTokensRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new ResourceNotFoundException("해당 리프레시 토큰을 가진 사용자가 없습니다."));

        User user = userTokens.getUser();

        // 이전 리프레시 토큰 저장 (필요 시 블랙리스트 처리를 위해)
        String oldRefreshToken = userTokens.getRefreshToken();

        // 새 토큰 생성
        TokenResponse newTokens = createTokens(user.getEmail());

        return newTokens;
    }

    // 로그아웃 시 토큰 무효화
    @Transactional
    public void logout(String userCode) {
        UserTokens userTokens = userTokensRepository.findById(userCode)
                .orElseThrow( () -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

            userTokens.setAccessToken(null);
            userTokens.setRefreshToken(null);
            userTokens.setTokenExpiresAt(null);
            userTokensRepository.save(userTokens);
    }

    // 만료된 토큰을 스케줄링으로 정리
    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정에 실행
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        List<UserTokens> expiredTokens = userTokensRepository.findByTokenExpiresAtBefore(now);

        for (UserTokens token : expiredTokens) {
            token.setRefreshToken(null);
            token.setAccessToken(null);
            userTokensRepository.save(token);
        }

        log.info("만료된 토큰 {}개가 정리 되었습니다.", expiredTokens.size());
    }
} 