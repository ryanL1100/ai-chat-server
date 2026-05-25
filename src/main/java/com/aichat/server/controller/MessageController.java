package com.aichat.server.controller;

import com.aichat.server.dto.request.MessageRequest;
import com.aichat.server.dto.response.ApiResponse;
import com.aichat.server.dto.response.PageResponse;
import com.aichat.server.entity.Message;
import com.aichat.server.repository.ChatRepository;
import com.aichat.server.repository.MessageRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;

    /**
     * GET /api/messages/{chatId}
     * 支持游标分页（before 参数）
     */
    @GetMapping("/{chatId}")
    public ApiResponse<PageResponse<Message>> getList(
            @AuthenticationPrincipal String userId,
            @PathVariable String chatId,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Long after) {

        // 验证对话归属
        chatRepository.findById(chatId)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("对话不存在或无权访问"));

        Page<Message> page;
        if (before != null) {
            page = messageRepository.findByChatIdBeforeCursor(chatId, before, PageRequest.of(0, pageSize));
        } else {
            page = messageRepository.findByChatIdOrderByCreatedAtDesc(chatId, PageRequest.of(0, pageSize));
        }

        // 返回时按时间正序
        java.util.List<Message> list = new java.util.ArrayList<>(page.getContent());
        list.sort(java.util.Comparator.comparingLong(Message::getCreatedAt));

        return ApiResponse.success(PageResponse.<Message>builder()
                .list(list)
                .total(page.getTotalElements())
                .page(1)
                .pageSize(pageSize)
                .hasMore(page.hasNext())
                .build());
    }

    /**
     * POST /api/messages
     * 保存消息（用户消息 & AI 消息均通过此接口持久化）
     * 支持 clientMessageId 幂等去重：同一条消息重复提交时直接返回已存在的 id
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> save(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody MessageRequest request) {

        // 验证对话归属
        chatRepository.findById(request.getChatId())
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("对话不存在或无权访问"));

        // 幂等去重：如果 clientMessageId 已存在，直接返回已有消息的 id
        if (request.getClientMessageId() != null && !request.getClientMessageId().isBlank()) {
            java.util.Optional<Message> existing = messageRepository.findByClientMessageId(request.getClientMessageId());
            if (existing.isPresent()) {
                Message existingMsg = existing.get();
                Map<String, Object> idempotentResult = new HashMap<>();
                idempotentResult.put("messageId", existingMsg.getId());
                idempotentResult.put("timestamp", existingMsg.getCreatedAt());
                return ApiResponse.success(idempotentResult);
            }
        }

        // 优先使用前端传入的 createdAt，保持前后端时间一致
        long now = System.currentTimeMillis();
        long createdAt = (request.getCreatedAt() != null && request.getCreatedAt() > 0)
                ? request.getCreatedAt()
                : now;

        Message message = Message.builder()
                .chatId(request.getChatId())
                .role(request.getRole())
                .type(request.getType() != null ? request.getType() : "text")
                .content(request.getContent())
                .status("success")
                .isComplete(true)
                .isStarred(false)
                .metadata(request.getMetadata())
                .replyToId(request.getReplyToId())
                .clientMessageId(request.getClientMessageId())
                .createdAt(createdAt)
                .updatedAt(now)
                .build();

        message = messageRepository.save(message);

        // 更新对话的 lastMessage 和 messageCount
        chatRepository.findById(request.getChatId()).ifPresent(chat -> {
            chat.setLastMessage(request.getContent().length() > 100
                    ? request.getContent().substring(0, 100) + "..."
                    : request.getContent());
            chat.setLastMessageAt(now);
            chat.setMessageCount(chat.getMessageCount() + 1);
            chat.setUpdatedAt(now);
            chatRepository.save(chat);
        });

        Map<String, Object> result = new HashMap<>();
        result.put("messageId", message.getId());
        result.put("timestamp", createdAt);
        return ApiResponse.success(result);
    }

    /**
     * DELETE /api/messages/{id}
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal String userId,
            @PathVariable String id) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("消息不存在"));

        // 验证归属
        chatRepository.findById(message.getChatId())
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("无权删除此消息"));

        messageRepository.delete(message);
        return ApiResponse.success();
    }

    /**
     * PUT /api/messages/{id}/star
     * 收藏/取消收藏消息
     */
    @PutMapping("/{id}/star")
    public ApiResponse<Void> toggleStar(
            @AuthenticationPrincipal String userId,
            @PathVariable String id) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("消息不存在"));

        chatRepository.findById(message.getChatId())
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("无权操作此消息"));

        message.setIsStarred(!message.getIsStarred());
        message.setUpdatedAt(System.currentTimeMillis());
        messageRepository.save(message);
        return ApiResponse.success();
    }

    /**
     * POST /api/feedback
     * 消息反馈（点赞/踩/举报）
     */
    @PostMapping("/feedback")
    public ApiResponse<Void> feedback(
            @AuthenticationPrincipal String userId,
            @RequestBody Map<String, Object> body) {
        // TODO: 持久化反馈数据
        return ApiResponse.success();
    }
}
