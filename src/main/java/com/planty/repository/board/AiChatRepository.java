package com.planty.repository.board;

import com.planty.entity.board.AiChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiChatRepository extends JpaRepository<AiChat, Long> {}

