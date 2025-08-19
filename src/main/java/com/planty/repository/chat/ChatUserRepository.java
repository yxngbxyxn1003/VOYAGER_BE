package com.planty.repository.chat;


import com.planty.entity.chat.ChatUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatUserRepository extends JpaRepository<ChatUser, Long> {
    List<ChatUser> findByUserId(Integer userId);
    List<ChatUser> findByChatId(Long chatId);

    boolean existsByChatIdAndUserId(Long chatId, Integer senderId);

    List<ChatUser> findAllByUserId(Integer userId);
    List<ChatUser> findAllByChatId(Long chatId);

    @Query("SELECT cu FROM ChatUser cu WHERE cu.chat.id = :chatId AND cu.user.id = :userId")
    Optional<ChatUser> findByChatIdAndUserId(@Param("chatId") Long chatId, @Param("userId") Integer userId);


    void deleteByChatId(Long chatId);
}
