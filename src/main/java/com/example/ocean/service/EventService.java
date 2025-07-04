package com.example.ocean.service;

import com.example.ocean.domain.Event;
import com.example.ocean.mapper.EventMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService {

    @Autowired
    private EventMapper eventMapper;

    public List<Event> getTodayEvents(String userId) {
        return eventMapper.selectTodayEvents(userId);
    }

    public int countCompletedEventsThisWeekByWorkspace(String workspaceCd) {
        return eventMapper.countCompletedEventsThisWeekByWorkspace(workspaceCd);
    }

}
