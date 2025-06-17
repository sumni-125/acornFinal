package com.example.ocean.security.jwt;

import com.example.ocean.entity.UserTokens;
import com.example.ocean.repository.UserTokensRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Slf4j // 로깅을 위한 어노테이션
@Component // 스프링 빈으로 등록
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final UserTokensRepository userTokensRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private int jwtExpirationInMs;

    @Value("${app.jwt.refresh-expiration}")
    private int refreshTokenValidityInMs;

    private Key key;

    // 초기화 메서드
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT 키 초기화 완료");
    }

    // JWT 토큰 생성 메서드
    public String createToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    // 리프레시 토큰 생성
    public String createRefreshToken() {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenValidityInMs);

        return Jwts.builder()
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    // JWT 토큰에서 이메일 추출
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    // JWT 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }

    // 리프레시 토큰 검증 및 새 액세스 토큰 발급
    public String refreshToken(String refreshToken) {
        // 리프레시 토큰 유효성 검사
        if (validateToken(refreshToken)) {
            // 데이터베이스에서 리프레시 토큰 조회
            Optional<UserTokens> userTokensOptional = userTokensRepository.findByRefreshToken(refreshToken);
            
            if (userTokensOptional.isPresent()) {
                UserTokens userTokens = userTokensOptional.get();
                
                // 새 액세스 토큰 생성
                String newAccessToken = createToken(userTokens.getUsername());
                
                log.info("토큰 갱신 성공 - 사용자: {}", userTokens.getUsername());
                return newAccessToken;
            } else {
                log.warn("데이터베이스에서 리프레시 토큰을 찾을 수 없음");
            }
        }
        
        log.error("리프레시 토큰이 유효하지 않음");
        return null;
    }

    // 요청에서 JWT 토큰 추출
    public String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    /**
     * JWT 인증 필터
     */
    @Slf4j
    @RequiredArgsConstructor
    public static class JwtAuthenticationFilter extends OncePerRequestFilter {
        private final JwtTokenProvider tokenProvider;

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            try {
                String jwt = tokenProvider.getJwtFromRequest(request);
                
                if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                    String username = tokenProvider.getEmailFromToken(jwt);
                    
                    // 사용자 정보로 인증 객체 생성
                    UserDetails userDetails = new User(username, "", 
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
                    
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    
                    // SecurityContext에 인증 정보 설정
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("JWT 인증 성공 - 사용자: {}", username);
                }
            } catch (Exception ex) {
                log.error("JWT 인증 처리 중 오류 발생", ex);
                // 인증 실패 시 SecurityContext 초기화
                SecurityContextHolder.clearContext();
            }
            
            filterChain.doFilter(request, response);
        }
    }
}
