package com.aichat.server.controller;

import com.aichat.server.dto.request.ChatRequest;
import com.aichat.server.dto.response.ApiResponse;
import com.aichat.server.dto.response.PageResponse;
import com.aichat.server.entity.Chat;
import com.aichat.server.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * POST /api/chat/create
     */
    @PostMapping("/create")
    public ApiResponse<Chat> create(
            @AuthenticationPrincipal String userId,
            @RequestBody(required = false) ChatRequest request) {
        if (request == null) request = new ChatRequest();
        return ApiResponse.success(chatService.create(userId, request));
    }

    /**
     * GET /api/chat/list
     */
    @GetMapping("/list")
    public ApiResponse<PageResponse<Chat>> getList(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(chatService.getList(userId, page, pageSize, status, keyword));
    }

    /**
     * PUT /api/chat/{id}
     */
    @PutMapping("/{id}")
    public ApiResponse<Chat> update(
            @AuthenticationPrincipal String userId,
            @PathVariable String id,
            @RequestBody ChatRequest request) {
        return ApiResponse.success(chatService.update(userId, id, request));
    }

    /**
     * DELETE /api/chat/{id}
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal String userId,
            @PathVariable String id) {
        chatService.delete(userId, id);
        return ApiResponse.success();
    }

    /**
     * POST /api/chat/export
     */
    @PostMapping("/export")
    public ApiResponse<Map<String, Object>> export(
            @AuthenticationPrincipal String userId,
            @RequestBody Map<String, String> body) {
        String chatId = body.get("chatId");
        String format = body.getOrDefault("format", "txt");
        return ApiResponse.success(chatService.export(userId, chatId, format));
    }

    /**
     * POST /api/chat/share
     */
    @PostMapping("/share")
    public ApiResponse<Map<String, Object>> share(
            @AuthenticationPrincipal String userId,
            @RequestBody Map<String, Object> body) {
        String chatId = (String) body.get("chatId");
        Integer expiresIn = body.get("expiresIn") != null ? ((Number) body.get("expiresIn")).intValue() : null;
        Boolean isPublic = (Boolean) body.getOrDefault("isPublic", true);
        return ApiResponse.success(chatService.share(userId, chatId, expiresIn, isPublic));
    }

    /**
     * GET /api/chat/search
     */
    @GetMapping("/search")
    public ApiResponse<PageResponse<Map<String, Object>>> search(
            @AuthenticationPrincipal String userId,
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(chatService.search(userId, query, page, pageSize));
    }
}
