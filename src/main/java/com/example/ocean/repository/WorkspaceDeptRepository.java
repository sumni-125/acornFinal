package com.example.ocean.repository;

import com.example.ocean.entity.WorkspaceDept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkspaceDeptRepository extends JpaRepository<WorkspaceDept, String> {

    @Query("SELECT wd FROM WorkspaceDept wd WHERE wd.workspace.workspaceCd = :workspaceCd")
    List<WorkspaceDept> findByWorkspaceCd(@Param("workspaceCd") String workspaceCd);
}
