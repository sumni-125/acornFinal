package com.example.ocean.dto.request;

import lombok.Data;

@Data
public class UploadFileRequest {
    private String  fileName;
    private String  filePath;
    private String  fileType;
    private long    fileSize;
}
