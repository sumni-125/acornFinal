package com.example.ocean.repository;


import com.example.ocean.dto.request.PersonalEventUpdateRequest;
import com.example.ocean.dto.response.Event;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CalendarEventRepository {
    List <com.example.ocean.dto.response.PersonalCalendarResponse> selectPersonalCalendar(@Param("userID")String userID);
    int insertPersonalEvent(com.example.ocean.dto.response.Event event);
    Event selectPersonalEvent(@Param("eventCd")String eventCd);
    int updatePersonalEvent(PersonalEventUpdateRequest event);
    int deletePersonalEvent(@Param("eventCd")String eventCd);

}
