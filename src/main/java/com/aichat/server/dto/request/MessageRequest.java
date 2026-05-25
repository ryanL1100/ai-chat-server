package com.aichat.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MessageRequest {

    @NotBlank(message = "chatId 不能为空")
    private String chatId;

    @NotBlank(message = "role 不能为空")
    private String role;

    private String type;

    @NotBlank(message = "content 不能为空")
    private String content;

    private String metadata;  // JSON 字符串
    private String replyToId;

    /**
     * 客户端生成的临时 id，用于幂等去重。
     * 如果同一个 clientMessageId 已经存在，直接返回已存在的消息 id。
     */
    private String clientMessageId;

    /**
     * 客户端消息创建时间（毫秒时间戳）。
     * 服务端优先使用此值，保持前后端时间一致。
     */
    private Long createdAt;
}
