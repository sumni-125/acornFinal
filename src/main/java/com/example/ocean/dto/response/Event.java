package com.example.ocean.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Event {
    private String eventCd;             // EVENT_CD (PK)
    private String workspaceCd;         // WORKSPACE_CD (FK)
    private String userId;              // USER_ID (FK)
    private String title;               // TITLE
    private String description;         // DESCRIPTION
    private LocalDateTime startDatetime; // START_DATETIME
    private LocalDateTime endDatetime;   // END_DATETIME
    private String color;               // COLOR (ENUM: RED ORANGE YELLOW GREEN BLUE GRAY)
    private String isShared;            // IS_SHARED ('0' or '1')
    private String progressStatus;      // PROGRESS_STATUS (BEFORE / ING / DONE)
    private String priority;            // PRIORITY (LOW / NORMAL / HIGH)
    private LocalDateTime createdDate;  // CREATED_DATE
}
