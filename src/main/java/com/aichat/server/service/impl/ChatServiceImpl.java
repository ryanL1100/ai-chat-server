package com.aichat.server.service.impl;

import com.aichat.server.dto.request.ChatRequest;
import com.aichat.server.dto.response.PageResponse;
import com.aichat.server.entity.Chat;
import com.aichat.server.entity.Message;
import com.aichat.server.repository.ChatRepository;
import com.aichat.server.repository.MessageRepository;
import com.aichat.server.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Chat create(String userId, ChatRequest request) {
        long now = System.currentTimeMillis();
        Chat chat = Chat.builder()
                .userId(userId)
                .title(request.getTitle() != null ? request.getTitle() : "新对话 " + formatDate(now))
                .status("active")
                .tags(tagsToJson(request.getTags()))
                .categoryId(request.getCategoryId())
                .isStarred(false)
                .isPinned(false)
                .messageCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return chatRepository.save(chat);
    }

    @Override
    public PageResponse<Chat> getList(String userId, int page, int pageSize, String status, String keyword) {
        PageRequest pageable = PageRequest.of(Math.max(0, page - 1), pageSize);
        Page<Chat> result = chatRepository.findByUserIdWithFilters(userId, status, keyword, pageable);

        return PageResponse.<Chat>builder()
                .list(result.getContent())
                .total(result.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .hasMore(result.hasNext())
                .build();
    }

    @Override
    public Chat update(String userId, String chatId, ChatRequest request) {
        Chat chat = getOwnedChat(userId, chatId);

        if (request.getTitle() != null) chat.setTitle(request.getTitle());
        if (request.getTags() != null) chat.setTags(tagsToJson(request.getTags()));
        if (request.getCategoryId() != null) chat.setCategoryId(request.getCategoryId());
        if (request.getIsStarred() != null) chat.setIsStarred(request.getIsStarred());
        if (request.getIsPinned() != null) chat.setIsPinned(request.getIsPinned());
        if (request.getStatus() != null) chat.setStatus(request.getStatus());
        chat.setUpdatedAt(System.currentTimeMillis());

        return chatRepository.save(chat);
    }

    @Override
    @Transactional
    public void delete(String userId, String chatId) {
        Chat chat = getOwnedChat(userId, chatId);
        messageRepository.deleteByChatId(chatId);
        chatRepository.delete(chat);
    }

    @Override
    public Map<String, Object> export(String userId, String chatId, String format) {
        Chat chat = getOwnedChat(userId, chatId);
        List<Message> messages = messageRepository.findByChatIdOrderByCreatedAtAsc(chatId);

        // 生成导出内容（简化版，实际可写文件到 OSS）
        String content = buildExportContent(chat, messages, format);
        long expiresAt = System.currentTimeMillis() + 24 * 3600 * 1000L;

        // 实际项目：将 content 写入临时文件，返回下载 URL
        // 此处返回 base64 data URL 供前端直接使用
        String dataUrl = "data:text/plain;charset=utf-8;base64," +
                Base64.getEncoder().encodeToString(content.getBytes());

        Map<String, Object> result = new HashMap<>();
        result.put("url", dataUrl);
        result.put("expiresAt", expiresAt);
        result.put("format", format);
        return result;
    }

    @Override
    public Map<String, Object> share(String userId, String chatId, Integer expiresIn, Boolean isPublic) {
        getOwnedChat(userId, chatId);  // 验证归属
        String shareId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        long expiresAt = System.currentTimeMillis() + (expiresIn != null ? expiresIn * 1000L : 7 * 24 * 3600 * 1000L);

        Map<String, Object> result = new HashMap<>();
        result.put("shareUrl", "http://localhost:9090/share/" + shareId);
        result.put("shareId", shareId);
        result.put("expiresAt", expiresAt);
        return result;
    }

    @Override
    public PageResponse<Map<String, Object>> search(String userId, String query, int page, int pageSize) {
        PageRequest pageable = PageRequest.of(Math.max(0, page - 1), pageSize);
        Page<Message> messages = messageRepository.searchByContent(userId, query, pageable);

        List<Map<String, Object>> results = messages.getContent().stream().map(m -> {
            Map<String, Object> item = new HashMap<>();
            item.put("messageId", m.getId());
            item.put("chatId", m.getChatId());
            item.put("content", m.getContent());
            item.put("createdAt", m.getCreatedAt());
            // 高亮片段
            String content = m.getContent();
            int idx = content.toLowerCase().indexOf(query.toLowerCase());
            String highlight = idx >= 0
                    ? content.substring(Math.max(0, idx - 20), Math.min(content.length(), idx + query.length() + 20))
                    : content.substring(0, Math.min(80, content.length()));
            item.put("highlights", List.of(highlight));
            chatRepository.findById(m.getChatId()).ifPresent(c -> item.put("chatTitle", c.getTitle()));
            return item;
        }).collect(Collectors.toList());

        return PageResponse.<Map<String, Object>>builder()
                .list(results)
                .total(messages.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .hasMore(messages.hasNext())
                .build();
    }

    // ── 私有工具 ──────────────────────────────────────────────

    private Chat getOwnedChat(String userId, String chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("对话不存在"));
        if (!chat.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此对话");
        }
        return chat;
    }

    private String tagsToJson(List<String> tags) {
        if (tags == null || tags.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String formatDate(long ts) {
        java.time.LocalDate date = java.time.Instant.ofEpochMilli(ts)
                .atZone(java.time.ZoneId.of("Asia/Shanghai")).toLocalDate();
        return date.toString();
    }

    private String buildExportContent(Chat chat, List<Message> messages, String format) {
        return switch (format) {
            case "json" -> buildJsonExport(chat, messages);
            case "md" -> buildMarkdownExport(chat, messages);
            case "html" -> buildHtmlExport(chat, messages);
            default -> buildTxtExport(chat, messages);
        };
    }

    private String buildTxtExport(Chat chat, List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("对话标题：").append(chat.getTitle()).append("\n\n");
        for (Message m : messages) {
            sb.append("[").append("user".equals(m.getRole()) ? "用户" : "AI").append("]\n");
            sb.append(m.getContent()).append("\n\n");
        }
        return sb.toString();
    }

    private String buildMarkdownExport(Chat chat, List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(chat.getTitle()).append("\n\n");
        for (Message m : messages) {
            sb.append("**").append("user".equals(m.getRole()) ? "用户" : "AI").append("**\n\n");
            sb.append(m.getContent()).append("\n\n---\n\n");
        }
        return sb.toString();
    }

    private String buildJsonExport(Chat chat, List<Message> messages) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("chat", chat);
            data.put("messages", messages);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String buildHtmlExport(Chat chat, List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>")
          .append(chat.getTitle()).append("</title></head><body>");
        sb.append("<h1>").append(chat.getTitle()).append("</h1>");
        for (Message m : messages) {
            String role = "user".equals(m.getRole()) ? "用户" : "AI";
            sb.append("<div><strong>").append(role).append("</strong><p>")
              .append(m.getContent().replace("\n", "<br>")).append("</p></div><hr>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }
}
