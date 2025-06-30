package com.example.ocean.repository;

import com.example.ocean.dto.request.UpdateTeamEventRequest;
import com.example.ocean.dto.response.TeamCalendarResponse;
import com.example.ocean.dto.response.TeamEventDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TeamEventRepository {
    List<TeamCalendarResponse> selectTeamEvents(@Param("workspaceCd") String workspaceCd);
    TeamEventDetail selectTeamEventDetail(@Param("eventCd") String eventCd);
    int insertTeamEvent(TeamEventDetail teamEventDetail);
    int updateTeamEvent(UpdateTeamEventRequest updateTeamEventRequest);
    int deleteTeamEvent(@Param("eventCd") String eventCd, @Param("userId") String userId);
}
