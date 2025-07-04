package com.example.ocean.dto.response;

import com.example.ocean.domain.Event;
import com.example.ocean.domain.File;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
@Data
@AllArgsConstructor
public class EventDetailResponse {
    private Event event;
    private List<AttendeesInfo> attendences;
    private List<File> fileList;
}
