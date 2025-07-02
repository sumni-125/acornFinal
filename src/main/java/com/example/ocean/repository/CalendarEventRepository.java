package com.example.ocean.repository;


import com.example.ocean.dto.request.PersonalEventUpdateRequest;
import com.example.ocean.dto.response.Event;
import com.example.ocean.dto.response.MailInfo;
import com.example.ocean.dto.response.PersonalCalendarResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.security.core.parameters.P;

import java.util.List;

@Mapper
public interface CalendarEventRepository {
    List <PersonalCalendarResponse> selectPersonalCalendar(@Param("userID")String userID);
    int insertPersonalEvent(Event event);
    Event selectPersonalEvent(@Param("eventCd")String eventCd);
    int updatePersonalEvent(PersonalEventUpdateRequest event);
    int deletePersonalEvent(@Param("eventCd")String eventCd);

    List<String> selectTodayAlarm();
    MailInfo selectMailInfo(@Param("eventCd") String eventCd);
}
