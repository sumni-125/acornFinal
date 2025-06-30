package com.example.ocean.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InsertAttendence {
    private String eventCd;
    private String userId;
}
