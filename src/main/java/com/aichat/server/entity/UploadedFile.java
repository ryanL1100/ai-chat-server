package com.aichat.server.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "uploaded_files")
public class UploadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId;

    private String chatId;

    @Column(nullable = false, length = 200)
    private String originalName;

    @Column(nullable = false, length = 200)
    private String storedName;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false, length = 50)
    private String mimeType;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false)
    private Long createdAt;
}
