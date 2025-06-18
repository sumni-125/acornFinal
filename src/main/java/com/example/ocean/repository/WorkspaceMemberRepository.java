package com.example.ocean.repository;

import com.example.ocean.entity.WorkspaceMember;
import com.example.ocean.entity.WorkspaceMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, WorkspaceMemberId> {

    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.user.userId = :userId AND wm.activeState = 'Y'")
    List<WorkspaceMember> findActiveWorkspacesByUserId(@Param("userId") String userId);

    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.workspace.workspaceCd = :workspaceCd AND wm.activeState = 'Y'")
    List<WorkspaceMember> findActiveMembers(@Param("workspaceCd") String workspaceCd);

    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.user.userId = :userId AND wm.workspace.workspaceCd = :workspaceCd")
    Optional<WorkspaceMember> findByUserIdAndWorkspaceCd(@Param("userId") String userId, @Param("workspaceCd") String workspaceCd);

    // 사용자의 첫 번째 워크스페이스 정보 가져오기 (프로필용)
    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.user.userId = :userId AND wm.activeState = 'Y' ORDER BY wm.joinedDate ASC")
    List<WorkspaceMember> findFirstWorkspaceInfo(@Param("userId") String userId);
}
