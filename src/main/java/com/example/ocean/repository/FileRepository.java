package com.example.ocean.repository;

import com.example.ocean.dto.request.CreateEventRequest;
import com.example.ocean.dto.response.FileEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FileRepository {

    int insertFile(FileEntity fileEntity);
    List<FileEntity> selectFileByEventCd(@Param("eventCd")String eventCd);
    FileEntity selectFileByFileId(@Param("fileId")String fileId);
    int updateFileActive(@Param("eventCd")String eventCd, @Param("fileId")String fileId);
    void deleteFileByEventCd(@Param("eventCd")String eventCd);
}
