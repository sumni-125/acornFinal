package com.example.ocean.controller.personalCalendar;


import com.example.ocean.dto.request.CreateEventRequest;
import com.example.ocean.dto.request.PersonalEventUpdateRequest;
import com.example.ocean.dto.response.PersonalCalendarResponse;
import com.example.ocean.dto.response.PersonalEventDetailResponse;

import com.example.ocean.service.PersonalCalendarService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/calendar/personal")
@RequiredArgsConstructor
public class PersonalCalendarAPIController {

    private final PersonalCalendarService personalCalendarService;

    @GetMapping("")
    public ResponseEntity<List<PersonalCalendarResponse>> personalCalendar(
            @RequestParam(required = false) String userId
    ) {
        userId="USR001";
        List<PersonalCalendarResponse> result = personalCalendarService.selectPersonalCalendar(userId);
        if (result == null || result.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createEvent(
            @RequestPart("request") CreateEventRequest createEventRequest,
            @RequestPart(value = "files", required = false) MultipartFile[] files

    ) {
        createEventRequest.setWorkspaceCd("WS001");
        System.out.println(createEventRequest);
        int result = personalCalendarService.createPersonalEvent(createEventRequest, files);

        return result == 1
                ? ResponseEntity.ok("일정 등록 성공")
                : ResponseEntity.badRequest().body("일정 등록 실패");
    }

    @GetMapping("/events/{eventCd}")
    public ResponseEntity<PersonalEventDetailResponse> getEventDetail(@PathVariable String eventCd) {
        PersonalEventDetailResponse result = personalCalendarService.getPersonalEventDetail(eventCd);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }
    @GetMapping("/events/{eventCd}/files")
    public ResponseEntity<byte[]> downloadFile(@RequestParam("fileId") String fileId) throws IOException {
        return personalCalendarService.downloadFile(fileId);
    }

    @DeleteMapping("/events/{eventCd}")
    public ResponseEntity<?> deleteFile(
            @PathVariable String eventCd
    ) {
        personalCalendarService.deletePersonalEvent(eventCd);
        return null;
    }

    @PutMapping("/events/{eventCd}")
    public ResponseEntity<String> updateEventDetail(
            @PathVariable String eventCd,
            @RequestPart("request") PersonalEventUpdateRequest request,
            @RequestPart(required = false) List<MultipartFile> files,
            @RequestPart(required = false) List<String> deletedFileIds,
            @RequestPart(required = false) List<String> deletedAttendences
    ) {
        //System.out.println(request);

        int result = personalCalendarService.updatePersonalEvent(request);
        boolean deletedF = personalCalendarService.updateFileActive(eventCd, deletedFileIds);


        MultipartFile[] fileArray = files != null
                ? files.toArray(new MultipartFile[0])
                : new MultipartFile[0];

        boolean insertFile = personalCalendarService.insertFile(fileArray, eventCd, request.getUserId());

        if (result == 1 && deletedF && insertFile) {
            return ResponseEntity.ok("일정 수정 성공");
        } else if (result == 1 && (!deletedF || !insertFile)) {
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body("일정은 수정되었지만 일부 파일 처리 실패");
        } else if (result != 1 && (deletedF || insertFile)) {
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body("파일은 처리되었지만 일정 수정 실패");
        } else {
            return ResponseEntity.badRequest().body("일정 수정 실패");
        }
    }
}