package com.aichat.server.controller;

import com.aichat.server.dto.request.LoginRequest;
import com.aichat.server.dto.request.RefreshTokenRequest;
import com.aichat.server.dto.request.RegisterRequest;
import com.aichat.server.dto.response.ApiResponse;
import com.aichat.server.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/login
     * 支持 phone / email / wechat / guest 登录
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /**
     * POST /api/auth/register
     * 注册新账号（phone / email）
     */
    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    /**
     * POST /api/auth/refresh
     * 刷新 accessToken
     */
    @PostMapping("/refresh")
    public ApiResponse<Map<String, Object>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    /**
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal String userId) {
        authService.logout(userId);
        return ApiResponse.success();
    }
}
