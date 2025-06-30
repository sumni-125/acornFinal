package com.example.ocean.repository;

import com.example.ocean.dto.response.EventUploadedFiles;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FileRepository {

    int insertFile(EventUploadedFiles eventUploadedFiles);
    List<EventUploadedFiles> selectFileByEventCd(@Param("eventCd")String eventCd);
    EventUploadedFiles selectFileByFileId(@Param("fileId")String fileId);
    int updateFileActive(@Param("eventCd")String eventCd, @Param("fileId")String fileId);
    void deleteFileByEventCd(@Param("eventCd")String eventCd);
}
