package com.example.ocean.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Notification {
    private String notiState;
    private String createdBy;
    private LocalDateTime createdDate;
}
