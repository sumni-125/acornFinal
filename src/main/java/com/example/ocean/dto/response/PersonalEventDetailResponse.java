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
    private String          notifyTime;     // 알림시간( 당일 오전 8시 / 전날 오후 8시 )
    private List<FileInfo>  fileList;
    private List<AttendeesInfo> attendeesInfo;
}
