package com.aichat.server.controller;

import com.aichat.server.dto.response.ApiResponse;
import com.aichat.server.entity.UploadedFile;
import com.aichat.server.repository.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FileController {

    private final UploadedFileRepository fileRepository;

    @Value("${storage.local.path:./uploads}")
    private String uploadPath;

    @Value("${storage.local.base-url:http://localhost:9090/files}")
    private String baseUrl;

    /**
     * POST /api/file/upload
     * 上传文件（图片/文档），返回可访问 URL
     */
    @PostMapping("/api/file/upload")
    public ApiResponse<Map<String, Object>> upload(
            @AuthenticationPrincipal String userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "chatId", required = false) String chatId) throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        long maxSize = 20 * 1024 * 1024L; // 20MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("文件大小不能超过 20MB");
        }

        // 确保上传目录存在
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // 生成唯一文件名
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String ext = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf("."))
                : "";
        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;

        // 保存文件
        Path targetPath = uploadDir.resolve(storedName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        String fileUrl = baseUrl + "/" + storedName;

        // 持久化记录
        UploadedFile record = UploadedFile.builder()
                .userId(userId)
                .chatId(chatId)
                .originalName(originalName)
                .storedName(storedName)
                .url(fileUrl)
                .mimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .size(file.getSize())
                .createdAt(System.currentTimeMillis())
                .build();
        fileRepository.save(record);

        // 对应前端 UploadFile 类型
        Map<String, Object> result = new HashMap<>();
        result.put("id", record.getId());
        result.put("name", originalName);
        result.put("url", fileUrl);
        result.put("size", file.getSize());
        result.put("type", file.getContentType());
        result.put("chatId", chatId);

        log.info("File uploaded: {} -> {}", originalName, storedName);
        return ApiResponse.success(result);
    }

    /**
     * GET /files/{filename}
     * 静态文件访问（无需鉴权，URL 本身即为凭证）
     */
    @GetMapping("/files/{filename}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadPath).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = "application/octet-stream";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
