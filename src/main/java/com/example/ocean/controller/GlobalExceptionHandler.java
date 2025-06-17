package com.example.ocean.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ModelAndView handleOAuth2Exception(OAuth2AuthenticationException e, HttpServletRequest request) {
        log.error("OAuth2 인증 오류 발생: {}", e.getMessage(), e);
        
        ModelAndView mav = new ModelAndView();
        mav.setViewName("redirect:/oauth2/redirect");
        mav.addObject("error", e.getMessage());
        
        return mav;
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception e, HttpServletRequest request) {
        log.error("일반 오류 발생 - URL: {}, 오류: {}", request.getRequestURL(), e.getMessage(), e);
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
    }
}