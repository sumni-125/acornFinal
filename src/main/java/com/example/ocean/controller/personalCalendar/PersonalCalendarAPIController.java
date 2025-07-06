package com.example.ocean.controller.personalCalendar;


import com.example.ocean.dto.request.EventCreateRequest;
import com.example.ocean.dto.request.EventUpdateRequest;
import com.example.ocean.dto.response.EventDetailResponse;
import com.example.ocean.dto.response.CalendarResponse;

import com.example.ocean.service.PersonalCalendarService;
import lombok.RequiredArgsConstructor;

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
    public ResponseEntity<List<CalendarResponse>> personalCalendar(
            @RequestParam(required = false) String userId,
            @RequestParam String workspaceCd
    ) {
        List<CalendarResponse> result = personalCalendarService.getPersonalEvents(userId, workspaceCd);

        if (result == null || result.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/events/{eventCd}")
    public ResponseEntity<EventDetailResponse> getEventDetail(@PathVariable String eventCd) {
        EventDetailResponse result = personalCalendarService.getPersonalEventDetail(eventCd);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> createEvent(
            @RequestPart("request") EventCreateRequest eventCreateRequest,
            @RequestParam(value = "attendenceIds", required = false) List<String> attendenceIds,
            @RequestPart(value = "files", required = false) MultipartFile[] files

    ) {
        int result = personalCalendarService.createPersonalEvent(eventCreateRequest, attendenceIds, files);

        return result == 1
                ? ResponseEntity.ok("일정 등록 성공")
                : ResponseEntity.badRequest().body("일정 등록 실패");
    }

    @PutMapping("/events/{eventCd}")
    public ResponseEntity<String> updateEventDetail(
            @PathVariable String eventCd,
            @RequestPart("request") EventUpdateRequest request,
            @RequestPart(required = false) MultipartFile[] files,
            @RequestPart(required = false) List<String> deletedFileIds,
            @RequestParam(value = "attendenceIds", required = false) List<String> attendenceIds
    ) {

        int result = personalCalendarService.updatePersonalEvent(request, attendenceIds, deletedFileIds, files);

        return result == 1
                ? ResponseEntity.ok("일정 수정 성공")
                : ResponseEntity.badRequest().body("일정 수정 실패");
    }

    @DeleteMapping("/events/{eventCd}")
    public ResponseEntity<String> deleteFile(
            @PathVariable String eventCd,
            @RequestParam(required = false) String userId
    ) {
        int result = personalCalendarService.deletePersonalEvent(eventCd, userId);
        return result == 1
                ? ResponseEntity.ok("일정 삭제 성공")
                : ResponseEntity.badRequest().body("일정 삭제 실패");
    }

    @GetMapping("/events/{eventCd}/files")
    public ResponseEntity<byte[]> downloadFile(@RequestParam("fileId") String fileId) throws IOException {
        return personalCalendarService.downloadFile(fileId);
    }

}