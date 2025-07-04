package com.example.ocean.domain;

import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
public class Workspace {
    private String workspaceCd;
    private String workspaceNm;
    private String workspaceImg;
    private String inviteCd;
    private String activeState;
    private Timestamp createdDate;
    private Timestamp endDate;
    private Integer favorite;
    private Timestamp entranceDate;


}
