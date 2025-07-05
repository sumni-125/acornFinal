package com.example.ocean.controller.event;

import com.example.ocean.domain.Event;
import com.example.ocean.mapper.MemberTransactionMapper;
import com.example.ocean.security.oauth.UserPrincipal;
import com.example.ocean.service.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private EventService eventService;

    @GetMapping("/today")
    public List<Event> getTodayEvents(@RequestParam("userId") String userId) {
        List<Event> result = eventService.getTodayEvents(userId);
        return result;
    }

    @GetMapping("/this-week-completed-count")
    public int getThisWeekCompletedCount(@RequestParam("workspaceCd") String workspaceCd) {
        int count = eventService.countCompletedEventsThisWeekByWorkspace(workspaceCd);
        return count;
    }

    @GetMapping("/this-week-upcoming-count")
    public int getThisWeekUpcomingCount(@RequestParam("workspaceCd") String workspaceCd) {
        int count = eventService.countUpcomingEventsThisWeekByWorkspace(workspaceCd);
        return count;
    }

    @GetMapping("/this-week-created-count")
    public int getThisWeekCreatedCount(@RequestParam("workspaceCd") String workspaceCd) {
        int count = eventService.countCreatedEventsThisWeekByWorkspace(workspaceCd);
        return count;
    }



}
