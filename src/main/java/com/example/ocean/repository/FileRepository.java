package com.example.ocean.repository;

import com.example.ocean.dto.request.InsertFileRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FileRepository {

    int insertFile(InsertFileRequest insertFileRequest);
    List<InsertFileRequest> selectFileByEventCd(@Param("eventCd")String eventCd);
    InsertFileRequest selectFileByFileId(@Param("fileId")String fileId);
    int updateFileActive(@Param("eventCd")String eventCd, @Param("fileId")String fileId);
    void deleteFileByEventCd(@Param("eventCd")String eventCd);
}
