package com.example.ocean.controller.personalCalendar;

import com.example.ocean.dto.request.CreateEventRequest;
import com.example.ocean.dto.request.PersonalEventUpdateRequest;
import com.example.ocean.dto.response.PersonalCalendarResponse;
import com.example.ocean.dto.response.PersonalEventDetailResponse;
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
    @GetMapping("/events/{eventCd}/download")
    public ResponseEntity<byte[]> downloadFile(@RequestParam("fileId") String fileId) throws IOException {
        return personalCalendarService.downloadFile(fileId);
    }

    @PutMapping("/events/{eventCd}")
    public ResponseEntity<String> updateEventDetail(@RequestBody PersonalEventUpdateRequest event) {
        int result = personalCalendarService.updatePersonalEvent(event);

        return result == 1
                ? ResponseEntity.ok("일정 수정 성공")
                : ResponseEntity.badRequest().body("일정 수정 실패");

    }
}