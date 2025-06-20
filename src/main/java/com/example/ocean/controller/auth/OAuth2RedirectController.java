package com.example.ocean.controller.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/oauth2")
public class OAuth2RedirectController {

    /**
     * OAuth2 인증 후 리다이렉트 페이지를 처리
     * 이 페이지는 인증 성공 후 토큰을 받아 로컬 스토리지에 저장하고
     * 메인 페이지로 리다이렉트
     *
     * @return OAuth2 리다이렉트 페이지
     */
    @GetMapping("/redirect")
    public String handleRedirect(@RequestParam Map<String, String> params,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        try {
            log.info("=== OAuth2 리다이렉트 처리 시작 ===");
            log.info("요청 URL: {}", request.getRequestURL());
            log.info("쿼리 파라미터: {}", params);

            // 토큰 유무 확인
            if (params.containsKey("token")) {
                log.info("토큰 발견: 길이 = {}", params.get("token").length());
            } else {
                log.warn("토큰이 없습니다!");
            }

            // 에러 확인
            if (params.containsKey("error")) {
                log.error("OAuth2 리다이렉트 에러: {}", params.get("error"));
                if (params.containsKey("message")) {
                    log.error("에러 메시지: {}", params.get("message"));
                }
            }

            // 템플릿 엔진 확인
            log.info("뷰 이름 반환: oauth2-redirect");

            // 헤더 정보 로깅
            log.debug("Accept 헤더: {}", request.getHeader("Accept"));
            log.debug("User-Agent: {}", request.getHeader("User-Agent"));

            // 응답 상태 확인
            log.info("응답 상태 설정 전 isCommitted: {}", response.isCommitted());

            return "oauth2-redirect";

        } catch (Exception e) {
            log.error("OAuth2 리다이렉트 처리 중 예외 발생", e);
            throw new RuntimeException("OAuth2 리다이렉트 처리 실패", e);
        }
    }
}