package com.example.ocean.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileUpdateRequest {
    private String nickname; // 닉네임
    private String phonNumer; // 전화번호
    private String department; // 부서
    private String position; // 직급
}
