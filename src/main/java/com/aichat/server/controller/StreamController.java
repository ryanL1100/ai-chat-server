package com.aichat.server.controller;

import com.aichat.server.dto.request.StreamRequest;
import com.aichat.server.dto.response.ApiResponse;
import com.aichat.server.entity.Message;
import com.aichat.server.repository.ChatRepository;
import com.aichat.server.repository.MessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class StreamController {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    @Value("${ai.base-url}")
    private String aiBaseUrl;

    @Value("${ai.app-id}")
    private String aiAppId;

    @Value("${ai.default-model}")
    private String defaultModel;

    @Value("${ai.timeout:60000}")
    private long aiTimeout;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * POST /api/chat/stream
     * SSE 流式接口：代理转发到 Friday One-API，并将完整回复持久化
     *
     * 前端 sseStream.ts 直接请求 Friday API，此接口作为可选的服务端代理
     * 优点：隐藏 AppId、统一鉴权、持久化消息
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody StreamRequest request) {

        // 验证对话归属
        chatRepository.findById(request.getChatId())
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("对话不存在或无权访问"));

        SseEmitter emitter = new SseEmitter(aiTimeout);

        executor.submit(() -> {
            StringBuilder fullContent = new StringBuilder();
            long startTime = System.currentTimeMillis();

            try {
                // 构建 OpenAI 格式请求体
                Map<String, Object> aiRequest = buildAiRequest(request);
                String requestBody = objectMapper.writeValueAsString(aiRequest);

                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(30))
                        .build();

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(aiBaseUrl.replaceAll("/$", "") + "/chat/completions"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + aiAppId)
                        .header("Accept", "text/event-stream")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .timeout(Duration.ofMillis(aiTimeout))
                        .build();

                HttpResponse<java.io.InputStream> response = client.send(
                        httpRequest, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                    String errBody = new String(response.body().readAllBytes());
                    sendError(emitter, "AI 服务请求失败 [" + response.statusCode() + "]: " + errBody);
                    return;
                }

                // 逐行读取 SSE
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(response.body()))) {

                    String line;
                    Map<String, Object> lastUsage = null;

                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();

                            if ("[DONE]".equals(data)) {
                                // 流结束，发送 complete 事件
                                Map<String, Object> completeEvent = new HashMap<>();
                                completeEvent.put("type", "complete");
                                completeEvent.put("metadata", buildMetadata(lastUsage, startTime, fullContent.length()));
                                emitter.send(SseEmitter.event()
                                        .name("message")
                                        .data(objectMapper.writeValueAsString(completeEvent)));
                                break;
                            }

                            try {
                                Map<?, ?> chunk = objectMapper.readValue(data, Map.class);
                                List<?> choices = (List<?>) chunk.get("choices");
                                if (choices != null && !choices.isEmpty()) {
                                    Map<?, ?> delta = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("delta");
                                    if (delta != null && delta.get("content") != null) {
                                        String content = (String) delta.get("content");
                                        fullContent.append(content);

                                        // 转发 chunk 给前端
                                        Map<String, Object> chunkEvent = new HashMap<>();
                                        chunkEvent.put("type", "chunk");
                                        chunkEvent.put("content", content);
                                        emitter.send(SseEmitter.event()
                                                .name("message")
                                                .data(objectMapper.writeValueAsString(chunkEvent)));
                                    }
                                }
                                if (chunk.get("usage") != null) {
                                    lastUsage = (Map<String, Object>) chunk.get("usage");
                                }
                            } catch (Exception parseEx) {
                                log.debug("SSE parse skip: {}", data);
                            }
                        }
                    }
                }

                // 持久化 AI 回复消息
                if (fullContent.length() > 0) {
                    saveAssistantMessage(request.getChatId(), fullContent.toString());
                }

                emitter.complete();

            } catch (Exception e) {
                log.error("SSE stream error", e);
                sendError(emitter, e.getMessage());
            }
        });

        return emitter;
    }

    /**
     * POST /api/feedback
     */
    @PostMapping("/feedback")
    public ApiResponse<Void> feedback(
            @AuthenticationPrincipal String userId,
            @RequestBody Map<String, Object> body) {
        return ApiResponse.success();
    }

    // ── 私有工具 ──────────────────────────────────────────────

    private Map<String, Object> buildAiRequest(StreamRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel() != null ? request.getModel() : defaultModel);
        body.put("stream", true);
        body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 4096);
        body.put("temperature", request.getTemperature() != null ? request.getTemperature() : 0.7);

        List<Map<String, String>> messages = request.getMessages().stream()
                .map(m -> {
                    Map<String, String> msg = new HashMap<>();
                    msg.put("role", m.getRole());
                    msg.put("content", m.getContent());
                    return msg;
                }).toList();
        body.put("messages", messages);
        return body;
    }

    private Map<String, Object> buildMetadata(Map<String, Object> usage, long startTime, int contentLength) {
        Map<String, Object> meta = new HashMap<>();
        if (usage != null) {
            meta.put("promptTokens", usage.get("prompt_tokens"));
            meta.put("completionTokens", usage.get("completion_tokens"));
            meta.put("totalTokens", usage.get("total_tokens"));
        }
        long elapsed = System.currentTimeMillis() - startTime;
        meta.put("latency", elapsed);
        return meta;
    }

    private void saveAssistantMessage(String chatId, String content) {
        long now = System.currentTimeMillis();
        Message message = Message.builder()
                .chatId(chatId)
                .role("assistant")
                .type("text")
                .content(content)
                .status("success")
                .isComplete(true)
                .isStarred(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
        messageRepository.save(message);

        chatRepository.findById(chatId).ifPresent(chat -> {
            chat.setLastMessage(content.length() > 100 ? content.substring(0, 100) + "..." : content);
            chat.setLastMessageAt(now);
            chat.setMessageCount(chat.getMessageCount() + 1);
            chat.setUpdatedAt(now);
            chatRepository.save(chat);
        });
    }

    private void sendError(SseEmitter emitter, String errorMsg) {
        try {
            Map<String, Object> errorEvent = new HashMap<>();
            errorEvent.put("type", "error");
            errorEvent.put("error", Map.of("code", "STREAM_ERROR", "message", errorMsg));
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(objectMapper.writeValueAsString(errorEvent)));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
