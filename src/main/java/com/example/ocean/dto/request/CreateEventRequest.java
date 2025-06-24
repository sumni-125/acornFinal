package com.example.ocean.dto.request;

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

    private List<String>    participantIds; // 작성자 외 참석자들 ID 리스트
    private int             notificationMinutes; // 알림시간 (예: [1440, 60])
    private List<UploadFileRequest> files; // 파일 업로드 요청
}
