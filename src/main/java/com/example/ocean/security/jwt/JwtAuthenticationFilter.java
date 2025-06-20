package com.example.ocean.security.jwt;

import com.example.ocean.entity.User;
import com.example.ocean.repository.UserRepository;
import com.example.ocean.security.oauth.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // OAuth2 관련 경로는 JWT 검증을 건너뛰기
        String requestUri = request.getRequestURI();
        if (requestUri.startsWith("/oauth2/") ||
                requestUri.startsWith("/login/oauth2/") ||
                requestUri.equals("/login")) {
            log.debug("OAuth2 경로이므로 JWT 필터를 건너뜁니다: {}", requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                // JWT에서 userId 추출
                String userId = jwtTokenProvider.getUserIdFromToken(jwt);

                // userId로 사용자 조회
                User user = userRepository.findById(userId)
                        .orElse(null);

                if (user != null) {
                    UserPrincipal userPrincipal = UserPrincipal.create(user);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("JWT 인증 성공 - userId: {}", userId);
                } else {
                    log.warn("JWT는 유효하지만 사용자를 찾을 수 없음 - userId: {}", userId);
                }
            }
        } catch (Exception ex) {
            log.error("사용자 인증을 설정할 수 없습니다", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}