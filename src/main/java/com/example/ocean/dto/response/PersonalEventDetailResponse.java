package com.example.ocean.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PersonalEventDetailResponse {

    private String          eventCd;
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

    private List<FileInfo> fileList;
    @Data
    public static class FileInfo {
        private String fileNm;
        private String fileId;
    }
}
