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

    private List<String>    participantIds; // 작성자 외 참석자들 ID 리스트
    private String          notifyTime;     // 알림시간( 당일 오전 8시 / 전날 오후 8시 )
    private List<UploadFileRequest> files; // 파일 업로드 요청

    @Data
    @Builder
    public static class FileEntity {
        private String fileId;
        private String eventCd;
        private String fileNm;
        private String fileType;
        private String filePath;
        private long   fileSize;
        private String uploadedBy;
        private LocalDateTime uploadedDate;
        private String activeState;
    }
}
