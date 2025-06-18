package com.example.ocean.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "WORKSPACE")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workspace {

    @Id
    @Column(name = "WORKSPACE_CD", length = 100)
    private String workspaceCd;

    @Column(name = "WORKSPACE_NM", nullable = false, length = 100)
    private String workspaceNm;

    @Column(name = "WORKSPACE_IMG", length = 255)
    private String workspaceImg;

    @Column(name = "INVITE_CD", unique = true, length = 12)
    private String inviteCd;

    @Column(name = "ACTIVE_STATE", length = 1)
    @Builder.Default
    private String activeState = "Y";

    @Column(name = "CREATED_DATE", updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "END_DATE")
    private LocalDateTime endDate;

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL)
    @Builder.Default
    private List<WorkspaceMember> members = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (workspaceCd == null) {
            workspaceCd = "WS" + System.currentTimeMillis();
        }
        if (inviteCd == null) {
            inviteCd = generateInviteCode();
        }
    }

    private String generateInviteCode() {
        // 12자리 랜덤 초대 코드 생성
        return java.util.UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }
}
