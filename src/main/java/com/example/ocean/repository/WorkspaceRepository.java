package com.example.ocean.repository;

import com.example.ocean.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, String> {
    Optional<Workspace> findByInviteCd(String inviteCd);
    boolean existsByWorkspaceCd(String workspaceCd);
}
