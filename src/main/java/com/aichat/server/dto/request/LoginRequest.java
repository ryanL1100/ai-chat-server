package com.aichat.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "登录类型不能为空")
    private String type;  // phone / email / wechat / apple / guest

    private String phone;
    private String email;
    private String password;
    private String code;    // 微信 code
    private String token;   // 第三方 token
}
