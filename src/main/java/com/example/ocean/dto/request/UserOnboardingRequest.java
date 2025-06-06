package com.example.ocean.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserOnboardingRequest {
    private String nickname;
    private String phoneNumber;
    private String department;
    private String positionl;
}
