package com.example.ocean.dto.request;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class CreateTeamEventRequest {
    private String          userId;
    private String          workspaceCd;
    private String          title;
    private String          description;
    private LocalDateTime   startDatetime;
    private LocalDateTime   endDatetime;
    private String          color;
    private String          progressStatus;
    private String          priority;
    private LocalDateTime   createdDate;
    private int             notifyTime;
}
