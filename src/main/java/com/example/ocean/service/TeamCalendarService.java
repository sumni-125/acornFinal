package com.example.ocean.service;

import com.example.ocean.dto.request.*;
import com.example.ocean.dto.response.*;
import com.example.ocean.repository.EventAttendencesRepository;
import com.example.ocean.repository.FileRepository;
import com.example.ocean.repository.MentionNotificationRepository;
import com.example.ocean.repository.TeamEventRepository;
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

@Service
@RequiredArgsConstructor
public class TeamCalendarService {

    private final TeamEventRepository teamEventRepository;
    private final EventAttendencesRepository eventAttendencesRepository;
    private final FileRepository fileRepository;
    private final MentionNotificationRepository mentionNotificationRepository;
    private final S3Uploader s3Uploader;

    public List<TeamCalendarResponse> getTeamEvents(String workspaceCd){
        return teamEventRepository.selectTeamEvents(workspaceCd);
    }

    public EventDetailResponse selectTeamEventDetail(String eventCd){
        List<AttendeesInfo> attendences = eventAttendencesRepository.selectAttendeesInfo(eventCd);
        TeamEventDetail event = teamEventRepository.selectTeamEventDetail(eventCd);
        List<EventUploadedFiles> fileList = fileRepository.selectFileByEventCd(eventCd);

        EventDetailResponse response = new EventDetailResponse(event, attendences, fileList);

        return response;
    }

    public int insertTeamEvent(CreateTeamEventRequest request, List<String> attendenceIds, MultipartFile[] files){
        String eventCd = "evnt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String userId = request.getUserId();

        TeamEventDetail detail = new TeamEventDetail();

        detail.setEventCd(eventCd);
        detail.setUserId(userId);
        detail.setWorkspaceCd(request.getWorkspaceCd());
        detail.setTitle(request.getTitle());
        detail.setDescription(request.getDescription());
        detail.setStartDatetime(request.getStartDatetime());
        detail.setEndDatetime(request.getEndDatetime());
        detail.setColor(request.getColor());
        detail.setIsShared("1");
        detail.setProgressStatus(request.getProgressStatus());
        detail.setPriority(request.getPriority());
        detail.setCreatedDate(LocalDateTime.now());
        detail.setNotifyTime(request.getNotifyTime());

        int events = teamEventRepository.insertTeamEvent(detail);

        // 참가자 삽입
        int attendences=0;
        if (attendenceIds != null && attendenceIds.size() > 0) {
            for(String attendId : attendenceIds){
                attendences+=eventAttendencesRepository.insertEventAttendences(eventCd, attendId);
            }

        }

        // 파일추가
        uploadFiles(eventCd, userId, files);

        insertMentionNotification(eventCd, "NEW");
        return events;
    }

    public int updateTeamEvent(UpdateTeamEventRequest updateTeamEventRequest, List<String> deletedFileIds, MultipartFile[] insertedFiles){
        int events = teamEventRepository.updateTeamEvent(updateTeamEventRequest);
        String eventCd = updateTeamEventRequest.getEventCd();
        String userId = updateTeamEventRequest.getUserId();

        //삭제된파일
        if (deletedFileIds != null && deletedFileIds.size() > 0) {
            for(String fileId:deletedFileIds){
                fileRepository.updateFileActiveByEventCdAndFileId(eventCd, fileId);
            }
        }

        //추가된파일
        uploadFiles(eventCd, userId, insertedFiles);

        insertMentionNotification(eventCd, "MODIFY");
        return events;
    }

    public int deleteTeamEvent(String eventCd, String userId){
        insertMentionNotification(eventCd, "DELETE");
        fileRepository.deleteFileByEventCd(eventCd);
        int attendences = eventAttendencesRepository.deleteAttendeesByEventCd(eventCd);
        int events = teamEventRepository.deleteTeamEvent(eventCd, userId);
        return events;
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
        if (attendeesInfos != null && attendeesInfos.size() > 0) {
            for(AttendeesInfo info :attendeesInfos){
                String attendId = info.getUserId();
                MentionNotification noti = new MentionNotification(eventCd, attendId, notiState,"0" );
                mentionNotificationRepository.insertMentionNotification(noti);
            }
        }

    }

}
