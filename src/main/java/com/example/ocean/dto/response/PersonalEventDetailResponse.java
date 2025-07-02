package com.example.ocean.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
@AllArgsConstructor
@Data
public class PersonalEventDetailResponse {

    private Event               event;
    private List<EventUploadedFiles>    fileList;
    private List<AttendeesInfo>         attendeesInfo;
}
