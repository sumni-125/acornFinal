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
                        .uploadedBy(request.getUserId())
                        .build();

                calendarEventRepository.insertFile(fileEntity);
            }
        }

        return result;
    }
    public PersonalEventDetailResponse getPersonalEventDetail(String eventCd){
        List<FileEntity> fileList = calendarEventRepository.selectFileEvent(eventCd);
        Event event = calendarEventRepository.selectPersonalEvent(eventCd);
        PersonalEventDetailResponse response = new PersonalEventDetailResponse();
        if (event != null) {
            response.setEventCd(event.getEventCd());
            response.setTitle(event.getTitle());
            response.setDescription(event.getDescription());
            response.setStartDatetime(event.getStartDatetime());
            response.setEndDatetime(event.getEndDatetime());
            response.setColor(event.getColor());
            response.setIsShared(event.getIsShared());
            response.setProgressStatus(event.getProgressStatus());
            response.setPriority(event.getPriority());
        }
        if (fileList != null) {
            List<PersonalEventDetailResponse.FileInfo> fileInfos = fileList.stream().map(file -> {
                PersonalEventDetailResponse.FileInfo fileInfo = new PersonalEventDetailResponse.FileInfo();
                fileInfo.setFileNm(file.getFileNm());
                fileInfo.setFileId(file.getFileId());
                return fileInfo;
            }).collect(Collectors.toList());
            response.setFileList(fileInfos);
        }

        return response;
    }

    public List<FileEntity> selectFileEvent(String eventCd){
        List<FileEntity> fileList = calendarEventRepository.selectFileEvent(eventCd);
        return calendarEventRepository.selectFileEvent(eventCd);
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
    private String extractKeyFromUrl(String url) {
        URI uri = URI.create(url);
        return uri.getPath().substring(1); // 앞에 '/' 제거
    }

    public int updatePersonalEvent(PersonalEventUpdateRequest event){
        return calendarEventRepository.updatePersonalEvent(event);
    }

}
