package com.aichat.server.controller;

import com.aichat.server.dto.response.ApiResponse;
import com.aichat.server.entity.User;
import com.aichat.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /**
     * GET /api/user/profile
     */
    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> getProfile(@AuthenticationPrincipal String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return ApiResponse.success(toUserMap(user));
    }

    /**
     * PUT /api/user/profile
     */
    @PutMapping("/profile")
    public ApiResponse<Map<String, Object>> updateProfile(
            @AuthenticationPrincipal String userId,
            @RequestBody Map<String, Object> updates) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if (updates.containsKey("nickname")) {
            user.setNickname((String) updates.get("nickname"));
        }
        if (updates.containsKey("avatar")) {
            user.setAvatar((String) updates.get("avatar"));
        }

        userRepository.save(user);
        return ApiResponse.success(toUserMap(user));
    }

    /**
     * GET /api/user/preferences
     * 返回默认偏好（实际项目可存数据库）
     */
    @GetMapping("/preferences")
    public ApiResponse<Map<String, Object>> getPreferences(@AuthenticationPrincipal String userId) {
        Map<String, Object> prefs = defaultPreferences();
        return ApiResponse.success(prefs);
    }

    /**
     * PUT /api/user/preferences
     */
    @PutMapping("/preferences")
    public ApiResponse<Void> updatePreferences(
            @AuthenticationPrincipal String userId,
            @RequestBody Map<String, Object> preferences) {
        // TODO: 持久化到数据库
        return ApiResponse.success();
    }

    /**
     * GET /api/user/stats
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats(@AuthenticationPrincipal String userId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalChats", 0);
        stats.put("totalMessages", 0);
        stats.put("totalTokens", 0);
        stats.put("joinedDays", 1);
        return ApiResponse.success(stats);
    }

    // ── 工具方法 ──────────────────────────────────────────────

    private Map<String, Object> toUserMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("nickname", user.getNickname());
        map.put("avatar", user.getAvatar());
        map.put("role", user.getRole());
        map.put("loginType", user.getLoginType());
        map.put("isGuest", user.getIsGuest());
        map.put("createdAt", user.getCreatedAt());
        map.put("lastLoginAt", user.getLastLoginAt());
        return map;
    }

    private Map<String, Object> defaultPreferences() {
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("theme", "system");
        prefs.put("language", "zh-CN");
        prefs.put("fontSize", "medium");
        prefs.put("timezone", "Asia/Shanghai");
        prefs.put("maxRetries", 3);
        prefs.put("confirmBeforeStop", false);
        prefs.put("confirmBeforeDelete", true);
        prefs.put("autoScroll", true);
        prefs.put("showTimestamp", true);
        prefs.put("timestampFormat", "relative");
        prefs.put("enableNotification", true);
        prefs.put("enableVoiceInput", true);
        prefs.put("enableHapticFeedback", true);
        prefs.put("cursorType", "line");
        prefs.put("blinkSpeed", 0.6);
        return prefs;
    }
}
