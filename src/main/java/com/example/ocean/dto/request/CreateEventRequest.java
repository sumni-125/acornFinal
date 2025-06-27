package com.example.ocean.dto.request;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateEventRequest {
    private String          userId;
    private String          workspaceCd;
    private String          title;
    private String          description;
    private LocalDateTime   startDatetime;
    private LocalDateTime   endDatetime;
    private String          color;
    private String          isShared;
    private String          progressStatus;
    private String          priority;
    private LocalDateTime   completeDateTime;
    private String          notifyTime;     // 알림시간( 당일 오전 8시 / 전날 오후 8시 )

}
