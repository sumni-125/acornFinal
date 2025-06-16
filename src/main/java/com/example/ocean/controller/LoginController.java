package com.example.ocean.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    /**
     * 로그인 페이지를 보여줍니다.
     * 
     * @return 로그인 페이지 템플릿
     */
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
} 