package com.aichat.server.service;

import com.aichat.server.dto.request.ChatRequest;
import com.aichat.server.dto.response.PageResponse;
import com.aichat.server.entity.Chat;

import java.util.Map;

public interface ChatService {
    Chat create(String userId, ChatRequest request);
    PageResponse<Chat> getList(String userId, int page, int pageSize, String status, String keyword);
    Chat update(String userId, String chatId, ChatRequest request);
    void delete(String userId, String chatId);
    Map<String, Object> export(String userId, String chatId, String format);
    Map<String, Object> share(String userId, String chatId, Integer expiresIn, Boolean isPublic);
    PageResponse<Map<String, Object>> search(String userId, String query, int page, int pageSize);
}
