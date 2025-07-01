package com.example.ocean.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ResponseStatus;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.PrintWriter;
import java.io.StringWriter;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ModelAndView handleOAuth2Exception(OAuth2AuthenticationException e, HttpServletRequest request) {
        log.error("OAuth2 인증 오류 발생");
        log.error("요청 URL: {}", request.getRequestURL());
        log.error("에러 메시지: {}", e.getMessage());
        
        if (e.getError() != null) {
            log.error("OAuth2 에러 코드: {}", e.getError().getErrorCode());
            log.error("OAuth2 에러 설명: {}", e.getError().getDescription());
            log.error("OAuth2 에러 URI: {}", e.getError().getUri());
        }
        
        log.error("전체 스택 트레이스:", e);
        
        ModelAndView mav = new ModelAndView();
        mav.setViewName("redirect:/oauth2/redirect");
        mav.addObject("error", e.getMessage());
        mav.addObject("error_type", "OAuth2AuthenticationException");
        
        if (e.getError() != null) {
            mav.addObject("error_code", e.getError().getErrorCode());
            mav.addObject("error_description", e.getError().getDescription());
        }
        
        return mav;
    }

    // 👇 추가: 404 처리
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResourceFound(NoResourceFoundException e, HttpServletRequest request, Model model) {
        log.warn("리소스 없음 - URL: {}, 오류: {}", request.getRequestURL(), e.getMessage());

        model.addAttribute("error", "404 Not Found");
        model.addAttribute("errorMessage", e.getMessage());
        model.addAttribute("exception", e.toString());
        return "error";  // templates/error/404.html 있으면 이 뷰 사용
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception e, HttpServletRequest request, Model model) {
        log.error("일반 오류 발생 - URL: {}, 오류: {}", request.getRequestURL(), e.getMessage(), e);
        
        // 에러 정보를 모델에 추가
        model.addAttribute("error", e.getClass().getSimpleName());
        model.addAttribute("errorMessage", e.getMessage());
        model.addAttribute("exception", e.toString());
        
        // 스택 트레이스를 문자열로 변환
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        model.addAttribute("trace", sw.toString());
        
        // OAuth2 관련 에러인 경우 특별 처리
        if (request.getRequestURI().contains("oauth2") || request.getRequestURI().contains("login")) {
            return "error/oauth-error";
        }
        
        return "error";
    }
}