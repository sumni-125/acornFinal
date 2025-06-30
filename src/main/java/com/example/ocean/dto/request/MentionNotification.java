package com.example.ocean.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MentionNotification {
    private String eventCd;
    private String userId;
    private String notiState;
    private String readState;
}
