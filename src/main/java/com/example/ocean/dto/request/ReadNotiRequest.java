package com.example.ocean.dto.request;

import lombok.Data;

@Data
public class ReadNotiRequest {
    private String eventCd;
    private String userId;
    private String notiState;
}
