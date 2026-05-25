package com.aichat.server.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String chatId;

    @Column(nullable = false, length = 20)
    private String role;  // user / assistant / system

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String type = "text";  // text / image / file / code / error

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "success";  // sending / streaming / success / error / stopped

    @Builder.Default
    @Column(name = "is_complete", nullable = false)
    @JsonProperty("isComplete")
    private Boolean isComplete = true;

    @Builder.Default
    @Column(name = "is_starred", nullable = false)
    @JsonProperty("isStarred")
    private Boolean isStarred = false;

    @Column(columnDefinition = "TEXT")
    private String metadata;  // JSON 字符串

    private String replyToId;

    /** 客户端生成的临时 id，用于幂等去重（可为 null，兼容旧数据） */
    @Column(unique = false)
    private String clientMessageId;

    @Column(nullable = false)
    private Long createdAt;

    @Column(nullable = false)
    private Long updatedAt;
}
