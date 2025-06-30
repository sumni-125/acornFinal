package com.example.ocean.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class TeamCalendarResponse {
    private String          eventCd;
    private String          title;
    private LocalDateTime   startDatetime;
    private LocalDateTime   endDatetime;
    private String          color;

}
