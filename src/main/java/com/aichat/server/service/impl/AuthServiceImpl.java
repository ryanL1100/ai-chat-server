package com.aichat.server.service.impl;

import com.aichat.server.dto.request.LoginRequest;
import com.aichat.server.dto.request.RefreshTokenRequest;
import com.aichat.server.dto.request.RegisterRequest;
import com.aichat.server.entity.User;
import com.aichat.server.repository.UserRepository;
import com.aichat.server.service.AuthService;
import com.aichat.server.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    // ── 登录 ──────────────────────────────────────────────────

    @Override
    public Map<String, Object> login(LoginRequest req) {
        User user = switch (req.getType()) {
            case "phone" -> loginByAccount(req.getPhone(), req.getPassword(), "phone");
            case "email" -> loginByAccount(req.getEmail(), req.getPassword(), "email");
            case "guest" -> loginAsGuest();
            case "wechat" -> loginByWechat(req.getCode());
            default -> throw new IllegalArgumentException("不支持的登录类型: " + req.getType());
        };
        return buildTokenResponse(user);
    }

    // ── 注册 ──────────────────────────────────────────────────

    @Override
    public Map<String, Object> register(RegisterRequest req) {
        String account = switch (req.getType()) {
            case "phone" -> {
                if (req.getPhone() == null || req.getPhone().isBlank())
                    throw new IllegalArgumentException("手机号不能为空");
                yield req.getPhone();
            }
            case "email" -> {
                if (req.getEmail() == null || req.getEmail().isBlank())
                    throw new IllegalArgumentException("邮箱不能为空");
                yield req.getEmail();
            }
            default -> throw new IllegalArgumentException("注册类型仅支持 phone / email");
        };

        // 账号已存在
        if (userRepository.findByAccount(account).isPresent()) {
            throw new IllegalArgumentException("该账号已注册，请直接登录");
        }

        String nickname = (req.getNickname() != null && !req.getNickname().isBlank())
                ? req.getNickname()
                : (account.contains("@") ? account.split("@")[0] : account);

        long now = System.currentTimeMillis();
        User user = User.builder()
                .account(account)
                .password(passwordEncoder.encode(req.getPassword()))
                .nickname(nickname)
                .loginType(req.getType())
                .isGuest(false)
                .role("user")
                .createdAt(now)
                .lastLoginAt(now)
                .build();
        user = userRepository.save(user);

        return buildTokenResponse(user);
    }

    // ── 刷新 Token ────────────────────────────────────────────

    @Override
    public Map<String, Object> refresh(RefreshTokenRequest req) {
        String token = req.getRefreshToken();
        if (!jwtUtil.isTokenValid(token)) {
            throw new IllegalArgumentException("refreshToken 无效或已过期");
        }
        String userId = jwtUtil.extractUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return buildTokenResponse(user);
    }

    // ── 登出 ──────────────────────────────────────────────────

    @Override
    public void logout(String userId) {
        // 无状态 JWT，登出只需客户端清除 token
    }

    // ── 私有方法 ──────────────────────────────────────────────

    /**
     * 账号密码登录（严格验证，账号不存在直接报错，不再自动注册）
     */
    private User loginByAccount(String account, String password, String loginType) {
        if (account == null || account.isBlank()) {
            throw new IllegalArgumentException("账号不能为空");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("密码长度不能少于 6 位");
        }

        User user = userRepository.findByAccount(account)
                .orElseThrow(() -> new IllegalArgumentException("账号不存在，请先注册"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("账号或密码错误");
        }

        user.setLastLoginAt(System.currentTimeMillis());
        return userRepository.save(user);
    }

    private User loginAsGuest() {
        String guestId = "guest_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        User guest = User.builder()
                .account(guestId)
                .nickname("游客")
                .loginType("guest")
                .isGuest(true)
                .role("user")
                .createdAt(System.currentTimeMillis())
                .lastLoginAt(System.currentTimeMillis())
                .build();
        return userRepository.save(guest);
    }

    private User loginByWechat(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("微信 code 不能为空");
        }
        // TODO: 调用微信 API 用 code 换取 openid
        String mockOpenId = "wx_" + code.substring(0, Math.min(8, code.length()));
        User user = userRepository.findByAccount(mockOpenId).orElse(null);
        if (user == null) {
            user = User.builder()
                    .account(mockOpenId)
                    .nickname("微信用户")
                    .loginType("wechat")
                    .isGuest(false)
                    .role("user")
                    .createdAt(System.currentTimeMillis())
                    .lastLoginAt(System.currentTimeMillis())
                    .build();
            user = userRepository.save(user);
        }
        return user;
    }

    private Map<String, Object> buildTokenResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("nickname", user.getNickname());
        userMap.put("avatar", user.getAvatar());
        String account = user.getAccount();
        userMap.put("email", account.contains("@") ? account : null);
        userMap.put("phone", (!account.contains("@") && !account.startsWith("guest_") && !account.startsWith("wx_")) ? account : null);
        userMap.put("role", user.getRole());
        userMap.put("loginType", user.getLoginType());
        userMap.put("isGuest", user.getIsGuest());
        userMap.put("createdAt", user.getCreatedAt());
        userMap.put("lastLoginAt", user.getLastLoginAt());

        Map<String, Object> result = new HashMap<>();
        result.put("user", userMap);
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("expiresIn", 3600);

        return result;
    }
}
