package com.aichat.server.repository;

import com.aichat.server.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

    /** 根据 clientMessageId 查找消息（用于幂等去重） */
    Optional<Message> findByClientMessageId(String clientMessageId);

    List<Message> findByChatIdOrderByCreatedAtAsc(String chatId);

    Page<Message> findByChatIdOrderByCreatedAtDesc(String chatId, Pageable pageable);

    // 游标分页：加载 before 时间戳之前的消息
    @Query("SELECT m FROM Message m WHERE m.chatId = :chatId AND m.createdAt < :before ORDER BY m.createdAt DESC")
    Page<Message> findByChatIdBeforeCursor(@Param("chatId") String chatId,
                                            @Param("before") Long before,
                                            Pageable pageable);

    long countByChatId(String chatId);

    // 全文搜索（简单 LIKE，生产可替换为 Elasticsearch）
    @Query("SELECT m FROM Message m WHERE m.chatId IN " +
           "(SELECT c.id FROM Chat c WHERE c.userId = :userId) " +
           "AND m.content LIKE %:query% ORDER BY m.createdAt DESC")
    Page<Message> searchByContent(@Param("userId") String userId,
                                   @Param("query") String query,
                                   Pageable pageable);

    void deleteByChatId(String chatId);
}
