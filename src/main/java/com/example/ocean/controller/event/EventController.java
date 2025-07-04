package com.example.ocean.controller.event;

import com.example.ocean.domain.Event;
import com.example.ocean.service.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        return eventService.countCompletedEventsThisWeekByWorkspace(workspaceCd);
    }

}
