package com.example.ocean.dto.request;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PersonalEventUpdateRequest {
    private String          eventCd;        //이벤트아이디(pk)
    private String          userId;         //작성자코드
    private String          title;          //제목
    private String          description;    //설명
    private LocalDateTime   startDatetime;  //시작일
    private LocalDateTime   endDatetime;    //종료일
    private String          color;          //색상
    private String          isShared;       //공개여부
    private String          progressStatus; //진행도(진행전 진행중 종료됨)
    private String          priority;       //중요도
    private LocalDateTime   completeDateTime;

    private List<String>    participantIds; // 작성자 외 참석자들 ID 리스트
    private int             notifyTime;     // 알림시간( 당일 오전 8시 / 전날 오후 8시 )
    private List<UploadFileRequest> files;  // 파일 업로드 요청
}
