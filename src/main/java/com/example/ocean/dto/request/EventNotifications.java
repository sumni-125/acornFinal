package com.example.ocean.dto.request;

import lombok.Data;

@Data
public class EventNotifications {
    private String notificationCd;
    private String eventCd;
    private String notifyTime;
}
