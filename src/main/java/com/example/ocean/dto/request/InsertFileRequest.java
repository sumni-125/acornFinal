package com.example.ocean.dto.request;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@Builder
public class InsertFileRequest {
    private String          fileId;
    private String          eventCd;
    private String          fileNm;
    private String          fileType;
    private String          filePath;
    private long            fileSize;
    private String          uploadedBy;
    private LocalDateTime   uploadedDate;
    private String          activeState;
}
