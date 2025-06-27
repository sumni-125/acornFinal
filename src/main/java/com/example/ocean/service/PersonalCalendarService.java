package com.example.ocean.service;

import com.example.ocean.dto.request.*;
import com.example.ocean.dto.response.*;
import com.example.ocean.repository.CalendarEventRepository;
import com.example.ocean.repository.EventAttendencesRepository;
import com.example.ocean.repository.FileRepository;
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
    private final EventAttendencesRepository eventAttendencesRepository;
    private final S3Uploader s3Uploader;

    public List<PersonalCalendarResponse> selectPersonalCalendar(String userId){ return calendarEventRepository.selectPersonalCalendar(userId);}

    // 일정 crud
    @Transactional
    public int createPersonalEvent(CreateEventRequest request, MultipartFile[] files){

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
        List<String> attendList = request.getParticipantIds();
        for(String attendUserId : attendList){
            EventAttendences attendence = new EventAttendences();
            String userNickname = eventAttendencesRepository.selectUserNicknameByUserId(attendUserId, request.getWorkspaceCd());

            attendence.setEventCd(eventCd);
            attendence.setUserId(attendUserId);
            attendence.setUserNickname(userNickname);
            insertAttendences(attendence);
        }

        int result = calendarEventRepository.insertPersonalEvent(event);

        insertFile(files, eventCd, request.getUserId());

        return result;
    }



    public PersonalEventDetailResponse getPersonalEventDetail(String eventCd){

        Event event = calendarEventRepository.selectPersonalEvent(eventCd);
        PersonalEventDetailResponse response = new PersonalEventDetailResponse();
        if (event != null) {
            response.setEventCd(event.getEventCd());
            response.setUserId(event.getUserId());
            response.setTitle(event.getTitle());
            response.setDescription(event.getDescription());
            response.setStartDatetime(event.getStartDatetime());
            response.setEndDatetime(event.getEndDatetime());
            response.setColor(event.getColor());
            response.setIsShared(event.getIsShared());
            response.setProgressStatus(event.getProgressStatus());
            response.setPriority(event.getPriority());
        }

        response.setParticipantIds(selectAttendenceIdByEventCd(eventCd));
        response.setFileList(selectFileEvent(eventCd));

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
        fileRepository.deleteFileByEventCd(eventCd);
        eventAttendencesRepository.deleteAttendencesByEventCd(eventCd);
        return calendarEventRepository.deletePersonalEvent(eventCd);
    }
    // 파일 crud
    public boolean insertFile(MultipartFile[] files, String eventCd, String userId) {
        int cnt=0;
        if (files != null && files.length > 0) {
            for (MultipartFile file : files) {
                String filePath = s3Uploader.upload(file, "event-files"); // S3 업로드

                FileEntity fileEntity = FileEntity.builder()
                        .fileId(UUID.randomUUID().toString())
                        .eventCd(eventCd)
                        .fileNm(file.getOriginalFilename())
                        .fileType(file.getContentType())
                        .filePath(filePath)
                        .fileSize(file.getSize())
                        .uploadedBy(userId)
                        .build();

                int insert = fileRepository.insertFile(fileEntity);
                cnt+=insert;
            }
        }
        if (files == null || files.length == 0) return true;

        return cnt==files.length;
    }

    public List<FileInfo> selectFileEvent(String eventCd){
        List<FileEntity> fileList = fileRepository.selectFileByEventCd(eventCd);
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
        FileEntity file = fileRepository.selectFileByFileId(fileId);
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

    public int insertAttendences(EventAttendences attendences){
        return eventAttendencesRepository.insertAttendences(attendences);
    }

    public List<EventAttendences> selectAttendenceIdByEventCd(String eventId){
        return eventAttendencesRepository.selectAttendencesByEventCd(eventId);
    }

    @Transactional
    public boolean deleteAttendencesByEventCdUserId(String eventCd, List<String> deletedUserIds){
        int cnt=0;
        for (String userId : deletedUserIds) {
            int deleted = eventAttendencesRepository.deleteAttendencesByEventCdUserId(eventCd, userId);
            cnt+=deleted;
        }
        return cnt == deletedUserIds.size();
    }

    public void deleteAttendencesByEventCd(String eventCd){
        eventAttendencesRepository.deleteAttendencesByEventCd(eventCd);
    }

}
