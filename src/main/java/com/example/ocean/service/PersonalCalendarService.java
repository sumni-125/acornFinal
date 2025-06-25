package com.example.ocean.service;

import com.example.ocean.dto.request.CreateEventRequest;
import com.example.ocean.dto.request.PersonalEventUpdateRequest;
import com.example.ocean.dto.response.PersonalCalendarResponse;
import com.example.ocean.dto.response.PersonalEventDetailResponse;
import com.example.ocean.dto.response.Event;
import com.example.ocean.dto.response.FileEntity;
import com.example.ocean.repository.CalendarEventRepository;
import com.example.ocean.util.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
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
    private final S3Uploader s3Uploader;

    public List<PersonalCalendarResponse> selectPersonalCalendar(String userId){ return calendarEventRepository.selectPersonalCalendar(userId);}

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

        int result = calendarEventRepository.insertPersonalEvent(event);

        insertFile(files, eventCd, request.getUserId());

        return result;
    }

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

                int insert = calendarEventRepository.insertFile(fileEntity);
                cnt+=insert;
            }
        }
        if (files == null || files.length == 0) return true;

        return cnt==files.length;
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
        response.setFileList(selectFileEvent(eventCd));

        return response;
    }

    public List<PersonalEventDetailResponse.FileInfo> selectFileEvent(String eventCd){
        List<FileEntity> fileList = calendarEventRepository.selectFileEvent(eventCd);
        if (fileList != null) {
            List<PersonalEventDetailResponse.FileInfo> fileInfos = fileList.stream().map(file -> {
                PersonalEventDetailResponse.FileInfo fileInfo = new PersonalEventDetailResponse.FileInfo();
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
        FileEntity file = calendarEventRepository.selectFileByFileId(fileId);
        String key = extractKeyFromUrl(file.getFilePath()); // 👈 핵심!
        byte[] bytes = s3Uploader.download(key);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + URLEncoder.encode(file.getFileNm(), "UTF-8") + "\"")
                .body(bytes);
    }
    public boolean updateFileActive(String eventCd, List<String> deletedFileIds){
        int cnt=0;
        for (String fileId : deletedFileIds) {
            int updated = calendarEventRepository.updateFileActive(eventCd, fileId);
            cnt+=updated;
        }
        return cnt == deletedFileIds.size();
    }
    private String extractKeyFromUrl(String url) {
        URI uri = URI.create(url);
        return uri.getPath().substring(1); // 앞에 '/' 제거
    }

    public int updatePersonalEvent(PersonalEventUpdateRequest event){
        return calendarEventRepository.updatePersonalEvent(event);
    }

    public int deletePersonalEvent(String eventCd){
        //파일먼저삭제하고? 이벤트 삭제하기
        calendarEventRepository.deleteFileByEventCd(eventCd);
        return calendarEventRepository.deletePersonalEvent(eventCd);
    }

}
