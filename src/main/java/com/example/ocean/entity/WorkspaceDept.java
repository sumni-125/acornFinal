package com.example.ocean.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "WORKSPACE_DEPT")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceDept {

    @Id
    @Column(name = "DEPT_CD", length = 10)
    private String deptCd;

    @Column(name = "DEPT_NM", nullable = false, length = 30)
    private String deptNm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WORKSPACE_CD", nullable = false)
    private Workspace workspace;
}
