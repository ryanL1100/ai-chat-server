package com.aichat.server.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class ChatRequest {
    private String title;
    private List<String> tags;
    private String categoryId;
    private Boolean isStarred;
    private Boolean isPinned;
    private String status;
}
