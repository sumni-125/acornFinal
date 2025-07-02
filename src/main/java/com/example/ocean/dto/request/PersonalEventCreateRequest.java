package com.example.ocean.dto.request;

import com.example.ocean.dto.response.EventUploadedFiles;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PersonalEventCreateRequest {
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
    private LocalDateTime   createdDate;
    private int             notifyTime;
}
