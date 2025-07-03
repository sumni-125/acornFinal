package com.example.ocean.repository;

import com.example.ocean.dto.request.MentionNotification;
import com.example.ocean.dto.request.ReadNotiRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MentionNotificationRepository {
    int insertMentionNotification(MentionNotification notification);
    List<MentionNotification> selectUserNoti(@Param("usreId") String userId);
    int updateAllNoti(@Param("usreId") String userId);

    int updateNoti(ReadNotiRequest request);
}
