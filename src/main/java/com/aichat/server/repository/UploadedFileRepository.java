package com.aichat.server.repository;

import com.aichat.server.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, String> {
    List<UploadedFile> findByChatId(String chatId);
    List<UploadedFile> findByUserId(String userId);
}
