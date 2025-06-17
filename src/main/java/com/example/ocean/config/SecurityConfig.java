package com.example.ocean.config;

import com.example.ocean.security.jwt.JwtTokenProvider;
import com.example.ocean.security.oauth.CustomOAuth2UserService;
import com.example.ocean.security.oauth.HttpCookieOAuth2AuthorizationRequestRepository;
import com.example.ocean.security.oauth.OAuth2AuthenticationFailureHandler;
import com.example.ocean.security.oauth.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

// TODO : 애플리케이션의 인증(Authentication)과  인가(Authorization)를 담당
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("SecurityConfig 초기화 중...");
        
        http
            // CSRF 보호 비활성화 (REST API에서는 일반적으로 필요 없음)
            .csrf(AbstractHttpConfigurer::disable)
            
            // CORS 설정 활성화
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 세션 관리 설정
            .sessionManagement(session -> {
                // 항상 세션 생성 (OAuth2 인증을 위해 필요)
                session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS);
                // 세션 고정 보호
                session.sessionFixation().changeSessionId();
                // 동시 세션 제어
                session.maximumSessions(1);
                // 세션 만료 시 리다이렉트
                session.invalidSessionUrl("/login");
            })
            
            // 요청 권한 설정
            .authorizeHttpRequests(auth -> auth
                // 인증 없이 접근 가능한 경로
                .requestMatchers("/", "/login", "/oauth2/**", "/api/auth/**", "/css/**", "/js/**", "/images/**").permitAll()
                // 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            
            // OAuth2 로그인 설정
            .oauth2Login(oauth2 -> oauth2
                // 로그인 페이지 설정
                .loginPage("/login")
                // 인증 요청 저장소 설정 (커스텀 구현체 사용)
                .authorizationEndpoint(endpoint -> endpoint
                    .baseUri("/oauth2/authorize")
                    .authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository)
                )
                // 리다이렉트 엔드포인트 설정
                .redirectionEndpoint(endpoint -> endpoint
                    .baseUri("/oauth2/callback/*")
                )
                // 사용자 정보 엔드포인트 설정
                .userInfoEndpoint(endpoint -> endpoint
                    .userService(customOAuth2UserService)
                )
                // 인증 성공/실패 핸들러 설정
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler)
            )
            
            // JWT 필터 추가
            .addFilterBefore(
                new JwtTokenProvider.JwtAuthenticationFilter(jwtTokenProvider),
                UsernamePasswordAuthenticationFilter.class
            );
        
        log.info("SecurityConfig 설정 완료");
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*")); // 모든 오리진 허용 (프로덕션에서는 특정 도메인으로 제한해야 함)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); // 쿠키 포함 요청 허용
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
