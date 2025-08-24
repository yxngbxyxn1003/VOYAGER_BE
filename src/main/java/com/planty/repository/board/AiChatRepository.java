package com.planty.repository.board;

import com.planty.entity.board.AiChat;
import com.planty.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiChatRepository extends JpaRepository<AiChat, Long> {
    Optional<AiChat> findByUser(User user);
}

