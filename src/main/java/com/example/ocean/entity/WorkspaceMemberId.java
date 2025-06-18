package com.example.ocean.entity;
// ========================================
// WorkspaceMemberId.java (복합키 클래스)
// ========================================

import lombok.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class WorkspaceMemberId implements Serializable {
    private String workspace;
    private String user;
}
