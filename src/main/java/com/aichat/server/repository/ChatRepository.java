package com.aichat.server.repository;

import com.aichat.server.entity.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRepository extends JpaRepository<Chat, String> {

    Page<Chat> findByUserIdAndStatusOrderByUpdatedAtDesc(String userId, String status, Pageable pageable);

    Page<Chat> findByUserIdOrderByUpdatedAtDesc(String userId, Pageable pageable);

    @Query("SELECT c FROM Chat c WHERE c.userId = :userId " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND (:keyword IS NULL OR c.title LIKE %:keyword% OR c.lastMessage LIKE %:keyword%) " +
           "ORDER BY c.updatedAt DESC")
    Page<Chat> findByUserIdWithFilters(
            @Param("userId") String userId,
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable);
}
