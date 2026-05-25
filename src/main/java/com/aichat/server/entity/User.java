package com.aichat.server.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true, length = 100)
    private String account;  // 手机号或邮箱

    @Column(length = 200)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(length = 500)
    private String avatar;

    @Column(length = 20)
    private String loginType;  // phone / email / wechat / apple / guest

    @Column(nullable = false)
    @Builder.Default
    @JsonProperty("isGuest")
    private Boolean isGuest = false;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String role = "user";

    @Column(nullable = false)
    private Long createdAt;

    @Column(nullable = false)
    private Long lastLoginAt;
}
