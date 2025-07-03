package com.example.ocean.dto.response;

import com.example.ocean.dto.request.InsertFileRequest;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
@Data
@AllArgsConstructor
public class EventDetailResponse {
    TeamEventDetail event;
    List<AttendeesInfo> attendences;
    List<EventUploadedFiles> fileList;
}
