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

    public List<PersonalCalendarResponse> getPersonalEvents(String userId){
        return calendarEventRepository.selectPersonalCalendar(userId);
    }

    @Transactional
    public int createPersonalEvent(PersonalEventCreateRequest request, List<String> attendenceIds, MultipartFile[] files){

        String eventCd = "evnt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String userId = request.getUserId();

        Event event = new Event();

        event.setEventCd(eventCd);
        event.setUserId(userId);
        event.setWorkspaceCd(request.getWorkspaceCd());
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setStartDatetime(request.getStartDatetime());
        event.setEndDatetime(request.getEndDatetime());
        event.setColor(request.getColor());
        event.setIsShared(request.getIsShared());
        event.setProgressStatus(request.getProgressStatus());
        event.setPriority(request.getPriority());
        event.setNotifyTime(request.getNotifyTime());
        event.setCreatedDate(LocalDateTime.now());

        int result = calendarEventRepository.insertPersonalEvent(event);

        // 참가자 삽입
        if (attendenceIds != null && !attendenceIds.isEmpty()) {
            for(String attendId : attendenceIds){
                eventAttendencesRepository.insertEventAttendences(eventCd, attendId);
            }
        }

        // 파일추가
        uploadFiles(eventCd, userId, files);

        insertMentionNotification(eventCd, "NEW");

        return result;
    }

    public PersonalEventDetailResponse getPersonalEventDetail(String eventCd){

        Event event = calendarEventRepository.selectPersonalEvent(eventCd);
        List<EventUploadedFiles> fileList = fileRepository.selectFileByEventCd(eventCd);
        List<AttendeesInfo> attendences = eventAttendencesRepository.selectAttendeesInfo(eventCd);

        PersonalEventDetailResponse response = new PersonalEventDetailResponse(event, fileList, attendences);

        return response;
    }

    @Transactional
    public int updatePersonalEvent(PersonalEventUpdateRequest personalEventUpdateRequest, List<String> deletedFileIds, MultipartFile[] insertedFiles){
        int events = calendarEventRepository.updatePersonalEvent(personalEventUpdateRequest);
        String eventCd = personalEventUpdateRequest.getEventCd();
        String userId = personalEventUpdateRequest.getUserId();

        //삭제된파일
        if (deletedFileIds != null && !deletedFileIds.isEmpty()) {
            for(String fileId:deletedFileIds){
                fileRepository.updateFileActiveByEventCdAndFileId(eventCd, fileId);
            }
        }

        //추가된파일
        uploadFiles(eventCd, userId, insertedFiles);
        //멘션
        insertMentionNotification(eventCd, "MODIFY");
        return events;
    }

    @Transactional
    public int deletePersonalEvent(String eventCd, String userId){
        //파일 참석자 먼저 삭제하고 이벤트 삭제하기
        insertMentionNotification(eventCd, "DELETE");

        fileRepository.deleteFileByEventCd(eventCd);
        eventAttendencesRepository.deleteAttendeesByEventCd(eventCd);
        return calendarEventRepository.deletePersonalEvent(eventCd);
    }

    public void uploadFiles(String eventCd, String userId, MultipartFile[] files) {
        //파일 s3 업로드 + 테이블에 삽입
        if (files != null && files.length > 0) {
            for (MultipartFile file : files) {
                String filePath = s3Uploader.upload(file, "event-files"); // S3 업로드
                EventUploadedFiles insertFileRequest = EventUploadedFiles.builder()
                        .fileId(UUID.randomUUID().toString())
                        .eventCd(eventCd)
                        .fileNm(file.getOriginalFilename())
                        .fileType(file.getContentType())
                        .filePath(filePath)
                        .fileSize(file.getSize())
                        .uploadedBy(userId)
                        .build();

                fileRepository.insertFile(insertFileRequest);

            }
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

    private String extractKeyFromUrl(String url) {
        URI uri = URI.create(url);
        return uri.getPath().substring(1); // 앞에 '/' 제거
    }

    private void insertMentionNotification(String eventCd, String notiState) {
        // 멘션
        List<AttendeesInfo> attendeesInfos=eventAttendencesRepository.selectAttendeesInfo(eventCd);

        if (attendeesInfos != null && !attendeesInfos.isEmpty()) {
            for(AttendeesInfo info :attendeesInfos){
                String attendId = info.getUserId();
                MentionNotification noti = new MentionNotification(eventCd, attendId, notiState,"0" );
                mentionNotificationRepository.insertMentionNotification(noti);
            }
        }

    }

}
