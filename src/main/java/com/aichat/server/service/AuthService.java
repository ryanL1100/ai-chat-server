package com.aichat.server.service;

import com.aichat.server.dto.request.LoginRequest;
import com.aichat.server.dto.request.RefreshTokenRequest;
import com.aichat.server.dto.request.RegisterRequest;

import java.util.Map;

public interface AuthService {
    Map<String, Object> login(LoginRequest request);
    Map<String, Object> register(RegisterRequest request);
    Map<String, Object> refresh(RefreshTokenRequest request);
    void logout(String userId);
}
