package com.example.ocean.security.oauth;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.micrometer.observation.GlobalObservationConvention;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import java.io.IOException;

public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtTokenProvider tokenProvider;
    private final String frontendUrl = "http://localhost:3000";

    private void onAuthenticationSuccess(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        String targetUrl = determineTargetUrl(request,response,authentication);

        if (response.isCommitted) {
            logger.debug("응답이 커밋 됐습니다.");
        }
    }
}
