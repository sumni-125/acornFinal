package com.example.ocean.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Event {
    private String eventCd;
    private String workspaceCd;
    private String userId;
    private String title;
    private String description;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private String color;
    private String isShared;
    private String progressStatus;
    private String priority;
    private LocalDateTime createdDate;
    private LocalDateTime notifyTime;
}