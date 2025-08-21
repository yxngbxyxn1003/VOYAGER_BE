package com.planty.repository.board;

import com.planty.entity.board.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {}