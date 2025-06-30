package com.example.ocean.service;

import com.example.ocean.dto.request.*;
import com.example.ocean.dto.response.*;
import com.example.ocean.repository.CalendarEventRepository;
import com.example.ocean.repository.EventAttendencesRepository;
import com.example.ocean.repository.FileRepository;
import com.example.ocean.repository.MentionNotificationRepository;
import com.example.ocean.util.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonalCalendarService {

    private final CalendarEventRepository calendarEventRepository;
    private final FileRepository fileRepository;
    private final S3Uploader s3Uploader;
    private final EventAttendencesRepository eventAttendencesRepository;
    private final MentionNotificationRepository mentionNotificationRepository;

    // 일정 crud

    public List<PersonalCalendarResponse> selectPersonalCalendar(String userId){ return calendarEventRepository.selectPersonalCalendar(userId);}

    @Transactional
    public int createPersonalEvent(PersonalEventCreateRequest request, List<String> attendenceIds, MultipartFile[] files){

        Event event = new Event();

        String eventCd = "evnt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        event.setEventCd(eventCd);
        event.setUserId(request.getUserId());
        event.setWorkspaceCd(request.getWorkspaceCd());
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setStartDatetime(request.getStartDatetime());
        event.setEndDatetime(request.getEndDatetime());
        event.setColor(request.getColor());
        event.setIsShared(request.getIsShared());
        event.setProgressStatus(request.getProgressStatus());
        event.setPriority(request.getPriority());
        event.setCreatedDate(LocalDateTime.now());

        System.out.println(event);


        int result = calendarEventRepository.insertPersonalEvent(event);

        insertAttendence(eventCd, attendenceIds);

        insertFile(files, eventCd, request.getUserId());

        return result;
    }



    public PersonalEventDetailResponse getPersonalEventDetail(String eventCd){

        Event event = calendarEventRepository.selectPersonalEvent(eventCd);
        PersonalEventDetailResponse response = new PersonalEventDetailResponse();
        if (event != null) {
            response.setEventCd(event.getEventCd());
            response.setUserId(event.getUserId());
            response.setWorkspaceCd(event.getWorkspaceCd());
            response.setTitle(event.getTitle());
            response.setDescription(event.getDescription());
            response.setStartDatetime(event.getStartDatetime());
            response.setEndDatetime(event.getEndDatetime());
            response.setColor(event.getColor());
            response.setIsShared(event.getIsShared());
            response.setProgressStatus(event.getProgressStatus());
            response.setPriority(event.getPriority());
            response.setCreatedDate(event.getCreatedDate());
            response.setNotifyTime(event.getNotifyTime());
        }

        response.setFileList(selectFileEvent(eventCd));
        response.setAttendeesInfo(selectAttendeesInfo(eventCd));

        return response;
    }

    @Transactional
    public int updatePersonalEvent(PersonalEventUpdateRequest event){
        if(event.getProgressStatus().equals("DONE")){
            event.setEndDatetime(LocalDateTime.now());
        }else{
            event.setEndDatetime(null);
        }

        return calendarEventRepository.updatePersonalEvent(event);
    }

    @Transactional
    public int deletePersonalEvent(String eventCd){
        //파일 참석자 먼저 삭제하고 이벤트 삭제하기
        
        List<EventAttendences> attendences = eventAttendencesRepository.selectAttendence(eventCd);
        
        List<String> userIds = attendences.stream()
                .map(EventAttendences::getUserId)
                .collect(Collectors.toList());
        
        //멘션알림
        createMentionNotification(eventCd, userIds, "DELETE");
        
        fileRepository.deleteFileByEventCd(eventCd);
        eventAttendencesRepository.deleteAttendeesByEventCd(eventCd);
        return calendarEventRepository.deletePersonalEvent(eventCd);
    }
    // 파일 crud
    public boolean insertFile(MultipartFile[] files, String eventCd, String userId) {
        int cnt=0;
        if (files != null && files.length > 0) {
            for (MultipartFile file : files) {
                String filePath = s3Uploader.upload(file, "event-files"); // S3 업로드

                EventUploadedFiles eventUploadedFiles = EventUploadedFiles.builder()
                        .fileId(UUID.randomUUID().toString())
                        .eventCd(eventCd)
                        .fileNm(file.getOriginalFilename())
                        .fileType(file.getContentType())
                        .filePath(filePath)
                        .fileSize(file.getSize())
                        .uploadedBy(userId)
                        .build();

                int insert = fileRepository.insertFile(eventUploadedFiles);
                cnt+=insert;
            }
        }
        if (files == null || files.length == 0) return true;

        return cnt==files.length;
    }

    public List<FileInfo> selectFileEvent(String eventCd){
        List<EventUploadedFiles> fileList = fileRepository.selectFileByEventCd(eventCd);
        if (fileList != null) {
            List<FileInfo> fileInfos = fileList.stream().map(file -> {
                FileInfo fileInfo = new FileInfo();
                fileInfo.setFileNm(file.getFileNm());
                fileInfo.setFileId(file.getFileId());
                return fileInfo;
            }).collect(Collectors.toList());

            return fileInfos;
        }else{
            return Collections.emptyList();
        }
    }

    public ResponseEntity<byte[]> downloadFile(String fileId) throws IOException {
        EventUploadedFiles file = fileRepository.selectFileByFileId(fileId);
        String key = extractKeyFromUrl(file.getFilePath());
        byte[] bytes = s3Uploader.download(key);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + URLEncoder.encode(file.getFileNm(), "UTF-8") + "\"")
                .body(bytes);
    }

    @Transactional
    public boolean updateFileActive(String eventCd, List<String> deletedFileIds){
        int cnt=0;
        for (String fileId : deletedFileIds) {
            int updated = fileRepository.updateFileActive(eventCd, fileId);
            cnt+=updated;
        }
        return cnt == deletedFileIds.size();
    }

    private String extractKeyFromUrl(String url) {
        URI uri = URI.create(url);
        return uri.getPath().substring(1); // 앞에 '/' 제거
    }

    // 참가자 테이블 crud
    @Transactional
    public boolean insertAttendence (String eventCd, List<String> attendenceIds){
        int cnt=0;
        if (attendenceIds == null || attendenceIds.isEmpty()) {
            // 선택된 참석자 없음, 그냥 리턴
            return true;
        }else{
            for(String attendId : attendenceIds){
                cnt+=eventAttendencesRepository.insertEventAttendences(eventCd, attendId);
            }
        }
        if(cnt==attendenceIds.size()){
            createMentionNotification(eventCd, attendenceIds, "NEW");
        }
        return  cnt==attendenceIds.size();
    }

    public boolean updateAttendences(String eventCd, List<String> updatedAttendees){
        List<EventAttendences> select = selectEventAttendences(eventCd);
        if (!select.isEmpty()) {
            deleteAttendences(eventCd);
        }

        boolean result = true;
        if (updatedAttendees != null && !updatedAttendees.isEmpty()) {
            result = insertAttendence(eventCd, updatedAttendees);
        }
        return result;
    }

    public List<EventAttendences> selectEventAttendences(String eventCd){
        return eventAttendencesRepository.selectAttendence(eventCd);
    }

    public List<AttendeesInfo> selectAttendeesInfo(String eventCd){
        return eventAttendencesRepository.selectAttendeesInfo(eventCd);
    }

    @Transactional
    public void deleteAttendences(String eventCd) {
        eventAttendencesRepository.deleteAttendeesByEventCd(eventCd);
    }
/*
    //워크스페이스 멤버 닉네임 select
    public List<WorkspaceMember> selectUserNicknameByWorkspaceCd(String workspaceCd){
        return eventAttendencesRepository.selectUserNicknameByWorkspaceCd(workspaceCd);
    }*/

    // 멘션 알림기능
    @Transactional
    public void createMentionNotification(String eventCd, List<String> participantIds, String notiState) {
        if (participantIds == null || participantIds.isEmpty()) {
            return;
        }
        for (String userId : participantIds) {
            MentionNotification mention = new MentionNotification(eventCd, userId, notiState, "0");
            mentionNotificationRepository.insertMentionNotification(mention);
        }
    }

    public  List<MentionNotification> selectUserNoti(String userId){
        return mentionNotificationRepository.selectUserNoti(userId);
    }

    public void updateAllUserNoti(String userId){
        mentionNotificationRepository.updateAllNoti(userId);
    }

    public void updateUserNoti(List<ReadNotiRequest> request){
        if(request != null){
            for(ReadNotiRequest noti : request){
                mentionNotificationRepository.updateNoti(noti);
            }
        }

    }

}
