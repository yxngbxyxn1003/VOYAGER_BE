package com.planty.repository.chat;

import com.planty.entity.chat.Chat;
import com.planty.entity.chat.ChatMessage;
import com.planty.entity.chat.ChatUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatIdOrderByCreatedAtAsc(Long chatId);
    List<ChatMessage> findByChatIdAndSenderId(Long chatId, Integer senderId);
    Optional<ChatMessage> findTopByChatIdOrderByCreatedAtDesc(Long chatId);

    void deleteByChatId(Long chatId);

    Integer countByChatIdAndSenderIdAndReadFalse(Long chatId, Integer userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE chat_message " +
            "SET is_read = true " +
            "WHERE chat_id = :chatId " +
            "AND sender_id <> :userId " +
            "AND is_read = false",
            nativeQuery = true)
    Integer markAsRead(@Param("chatId") Long chatId, @Param("userId") Integer userId);

}

