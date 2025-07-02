package com.example.ocean.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WorkspaceMemberRepository {
    String selectMemberEmail(@Param("workspaceCd")String workspaceCd, @Param("userId")String userId);
}
