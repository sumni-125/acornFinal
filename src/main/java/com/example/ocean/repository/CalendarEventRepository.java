package com.example.ocean.repository;

import com.example.ocean.dto.request.PersonalEventUpdateRequest;
import com.example.ocean.dto.response.PersonalCalendarResponse;
import com.example.ocean.dto.response.Event;
import com.example.ocean.dto.response.FileEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CalendarEventRepository {
    List <PersonalCalendarResponse> selectPersonalCalendar(@Param("userID")String userID);
    int insertPersonalEvent(Event event);
    int insertFile(FileEntity fileEntity);
    Event selectPersonalEvent(@Param("eventCd")String eventCd);
    List<FileEntity> selectFileEvent(@Param("eventCd")String eventCd);
    FileEntity selectFileByFileId(@Param("fileId")String fileId);
    int updatePersonalEvent(PersonalEventUpdateRequest event);

}
