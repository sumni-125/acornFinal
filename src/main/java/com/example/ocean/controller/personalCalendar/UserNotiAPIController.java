package com.example.ocean.controller.personalCalendar;

import com.example.ocean.dto.request.MentionNotification;
import com.example.ocean.dto.request.ReadNotiRequest;
import com.example.ocean.service.PersonalCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/userNoti")
@RequiredArgsConstructor
public class UserNotiAPIController {
    private final PersonalCalendarService personalCalendarService;
    @GetMapping("/{userId}")
    public ResponseEntity<List<MentionNotification>> getUserNoti(@PathVariable String userId){
        List<MentionNotification> result = personalCalendarService.selectUserNoti(userId);
        if (result == null || result.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{userId}")
    public void readAllNoti(@PathVariable String userId){
        personalCalendarService.updateAllUserNoti(userId);
    }
    @PutMapping("/read")
    public void readNoti(
            @RequestPart(required = false) List<ReadNotiRequest> request
    ){
        personalCalendarService.updateUserNoti(request);
    }
}
