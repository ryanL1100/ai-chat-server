package com.aichat.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class StreamRequest {

    @NotBlank(message = "chatId 不能为空")
    private String chatId;

    @NotEmpty(message = "messages 不能为空")
    private List<ChatMessageItem> messages;

    private String model;
    private Integer maxTokens;
    private Double temperature;

    @Data
    public static class ChatMessageItem {
        private String role;
        private String content;
    }
}
