package com.aichat.server.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "chats")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, length = 200)
    @Builder.Default
    private String title = "新对话";

    @Column(length = 20)
    @Builder.Default
    private String status = "active";  // active / archived / deleted

    @Column(length = 500)
    private String tags;  // JSON 数组字符串，如 ["tag1","tag2"]

    private String categoryId;

    @Builder.Default
    @JsonProperty("isStarred")
    private Boolean isStarred = false;

    @Builder.Default
    @JsonProperty("isPinned")
    private Boolean isPinned = false;

    @Column(length = 500)
    private String lastMessage;

    private Long lastMessageAt;

    @Builder.Default
    private Integer messageCount = 0;

    @Column(nullable = false)
    private Long createdAt;

    @Column(nullable = false)
    private Long updatedAt;
}
