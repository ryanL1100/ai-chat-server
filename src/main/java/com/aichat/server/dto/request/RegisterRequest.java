package com.aichat.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    /** 登录类型：phone / email */
    @NotBlank(message = "注册类型不能为空")
    private String type;

    /** 手机号（type=phone 时必填） */
    private String phone;

    /** 邮箱（type=email 时必填） */
    private String email;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度为 6-32 位")
    private String password;

    /** 昵称（可选） */
    @Size(max = 20, message = "昵称不能超过 20 个字符")
    private String nickname;
}
