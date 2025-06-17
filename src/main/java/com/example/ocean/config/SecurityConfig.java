package com.example.ocean.config;

import com.example.ocean.security.oauth.CustomOAuth2UserService;
import com.example.ocean.security.oauth.OAuth2AuthenticationFailureHandler;
import com.example.ocean.security.oauth.OAuth2AuthenticationSuccessHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// TODO : 애플리케이션의 인증(Authentication)과  인가(Authorization)를 담당
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    // 의존성 주입 ( 우리 로직으로 인증 검증 하기 , 성공 시 핸들러 , 실패시 핸들러 )
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    
    /**
     * 세션 문제를 해결하기 위한 커스텀 인증 요청 저장소
     * 표준 HttpSessionOAuth2AuthorizationRequestRepository를 확장하여 디버깅을 추가합니다.
     */
    public static class CustomAuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
        private static final String DEFAULT_AUTHORIZATION_REQUEST_ATTR_NAME = HttpSessionOAuth2AuthorizationRequestRepository.class
                .getName() + ".AUTHORIZATION_REQUEST";
        
        private final String sessionAttributeName;
        
        public CustomAuthorizationRequestRepository() {
            this.sessionAttributeName = DEFAULT_AUTHORIZATION_REQUEST_ATTR_NAME;
        }
        
        @Override
        public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
            Assert.notNull(request, "request cannot be null");
            HttpSession session = request.getSession(false);
            
            if (session != null) {
                Map<String, OAuth2AuthorizationRequest> authorizationRequests = getAuthorizationRequests(session);
                String stateParameter = getStateParameter(request);
                
                if (stateParameter != null) {
                    OAuth2AuthorizationRequest authRequest = authorizationRequests.get(stateParameter);
                    if (authRequest != null) {
                        log.debug("OAuth2 인증 요청 로드 성공 - state: {}", stateParameter);
                        return authRequest;
                    } else {
                        log.warn("OAuth2 인증 요청이 세션에 없음 - state: {}", stateParameter);
                    }
                } else {
                    log.warn("OAuth2 state 파라미터가 요청에 없음");
                }
            } else {
                log.warn("OAuth2 인증 요청 로드 실패 - 세션이 없음");
            }
            
            return null;
        }
        
        @Override
        public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request,
                                            jakarta.servlet.http.HttpServletResponse response) {
            Assert.notNull(request, "request cannot be null");
            Assert.notNull(response, "response cannot be null");
            
            if (authorizationRequest == null) {
                removeAuthorizationRequest(request, response);
                return;
            }
            
            HttpSession session = request.getSession(true);
            Map<String, OAuth2AuthorizationRequest> authorizationRequests = getAuthorizationRequests(session);
            authorizationRequests.put(authorizationRequest.getState(), authorizationRequest);
            session.setAttribute(this.sessionAttributeName, authorizationRequests);
            log.debug("OAuth2 인증 요청 저장 성공 - state: {}, 세션 ID: {}", 
                      authorizationRequest.getState(), session.getId());
        }
        
        @Override
        public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                   jakarta.servlet.http.HttpServletResponse response) {
            Assert.notNull(request, "request cannot be null");
            Assert.notNull(response, "response cannot be null");
            
            OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
            if (authorizationRequest != null) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    Map<String, OAuth2AuthorizationRequest> authorizationRequests = getAuthorizationRequests(session);
                    String stateParameter = getStateParameter(request);
                    if (stateParameter != null) {
                        authorizationRequests.remove(stateParameter);
                        log.debug("OAuth2 인증 요청 제거 성공 - state: {}", stateParameter);
                    }
                    if (authorizationRequests.isEmpty()) {
                        session.removeAttribute(this.sessionAttributeName);
                    } else {
                        session.setAttribute(this.sessionAttributeName, authorizationRequests);
                    }
                }
            }
            
            return authorizationRequest;
        }
        
        private String getStateParameter(HttpServletRequest request) {
            return request.getParameter("state");
        }
        
        @SuppressWarnings("unchecked")
        private Map<String, OAuth2AuthorizationRequest> getAuthorizationRequests(HttpSession session) {
            Map<String, OAuth2AuthorizationRequest> authorizationRequests = (Map<String, OAuth2AuthorizationRequest>) session
                    .getAttribute(this.sessionAttributeName);
            if (authorizationRequests == null) {
                authorizationRequests = new HashMap<>();
            }
            return authorizationRequests;
        }
    }
    
    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository() {
        return new CustomAuthorizationRequestRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) //CSRF비활성화 JWT가 CSRF를 막아주는 기능을 함
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS) // 항상 세션 생성
                )
                .authorizeHttpRequests(auth -> auth
                        // apu/auth/ :로그인,회원가입은 인증없이 접근 가능 , /oauth2/** : oauth2 callback URL
                        .requestMatchers("/", "/css/**", "/js/**","/images/**").permitAll()
                        .requestMatchers("/api/auth/**","/oauth2/**").permitAll() // OAuth2는 Spring Sercurity에서 처리
                        .requestMatchers("/debug/**").permitAll() // 디버그 엔드포인트
                        .requestMatchers("/api/**").authenticated() // 모든 API는 인증 필요
                        .anyRequest().permitAll() // 그 외는 허용 (정적 리소스 등등)
                )
                // Ex) 구글 소셜 로그인 시 URL : /oauth2/authorize/google , 구글 로그인 페이지로 리다이렉트
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login") // 로그인 페이지 설정
                        .authorizationEndpoint(authorization -> authorization
                                .baseUri("/oauth2/authorize") // 인증 요청 시작점
                                .authorizationRequestRepository(authorizationRequestRepository())
                        )
                        // 카카오 및 구글 인증 후 리다이렉트 URI
                        .redirectionEndpoint(redirection -> redirection
                                .baseUri("/login/oauth2/code/*")
                        )

                        // ex) 인증 성공 후 사용자 정보를 가져와 customOAuth2User...에서 처리 함
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )

                        // 로그인 페이지 설정 (OAuth2 인증 시작 전에 보여줄 페이지)
                        .loginPage("/login")
                        
                        // 모든 처리 완료 후 JWT토큰 생성 , 프론트엔드로 리다이렉트
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        //실패 시 에러 메시지와 함꼐 리다이렉트
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                );

    return http.build();
    }

}
