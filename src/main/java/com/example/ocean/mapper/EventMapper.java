package com.example.ocean.mapper;

import com.example.ocean.domain.Event;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EventMapper {
    List<Event> selectTodayEvents(@Param("userId") String userId);

    int countCompletedEventsThisWeekByWorkspace(@Param("workspaceCd") String workspaceCd);

}

