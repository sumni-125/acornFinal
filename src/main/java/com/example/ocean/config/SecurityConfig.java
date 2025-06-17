package com.example.ocean.config;

import com.example.ocean.security.oauth.CustomOAuth2UserService;
import com.example.ocean.security.oauth.OAuth2AuthenticationFailureHandler;
import com.example.ocean.security.oauth.OAuth2AuthenticationSuccessHandler;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// TODO : 애플리케이션의 인증(Authentication)과  인가(Authorization)를 담당
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // 의존성 주입 ( 우리 로직으로 인증 검증 하기 , 성공 시 핸들러 , 실패시 핸들러 )
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) //CSRF비활성화 JWT가 CSRF를 막아주는 기능을 함
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) //OAuth2 인증을 위해 세션 필요
                )
                .authorizeHttpRequests(auth -> auth
                        // apu/auth/ :로그인,회원가입은 인증없이 접근 가능 , /oauth2/** : oauth2 callback URL
                        .requestMatchers("/", "/css/**", "/js/**","/images/**").permitAll()
                        .requestMatchers("/api/auth/**","/oauth2/**").permitAll() // OAuth2는 Spring Sercurity에서 처리
                        .requestMatchers("/api/**").authenticated() // 모든 API는 인증 필요
                        .anyRequest().permitAll() // 그 외는 허용 (정적 리소스 등등)
                )
                // Ex) 구글 소셜 로그인 시 URL : /oauth2/authorize/google , 구글 로그인 페이지로 리다이렉트
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .baseUri("/oauth2/authorize") // 인증 요청 시작점
                        )
                        // 카카오 및 구글 인증 후 리다이렉트 URI
                        .redirectionEndpoint(redirection -> redirection
                                .baseUri("/login/oauth2/code/*")
                        )

                        // ex) 인증 성공 후 사용자 정보를 가져와 customOAuth2User...에서 처리 함
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )

                        // 모든 처리 완료 후 JWT토큰 생성 , 프론트엔드로 리다이렉트
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        //실패 시 에러 메시지와 함꼐 리다이렉트
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                );

    return http.build();
    }

}
