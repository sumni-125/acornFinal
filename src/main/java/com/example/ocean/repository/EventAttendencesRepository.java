package com.example.ocean.repository;

import com.example.ocean.dto.request.EventAttendences;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EventAttendencesRepository {

    String selectUserNicknameByUserId(@Param("userId")String userId, @Param("workspaceCd")String workspaceCd);

    int insertAttendences(EventAttendences attendences);
    List<EventAttendences> selectAttendencesByEventCd(@Param("eventCd")String eventCd);
    int deleteAttendencesByEventCdUserId(@Param("eventCd")String eventCd, @Param("userId")String userId);
    void deleteAttendencesByEventCd(@Param("eventCd")String eventCd);

}
