package com.example.ocean.controller.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/oauth2")
// TODO : Spirng Secruity가 내부적으로 처리 해주기 떄문에
//  엔드포인트를'/api/'로 지정 하지 않는다.

// /oauth2/* → OAuth2 표준 프로토콜
public class OAuth2Controller {

    //OAuth2 인증 시작점
    @GetMapping("/authorize/{provider}")
    public String authorize(@PathVariable String provider) {
        return "redirect:/oauth2/authorization/" + provider;
    }
}
