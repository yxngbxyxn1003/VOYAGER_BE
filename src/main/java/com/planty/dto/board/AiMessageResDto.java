package com.planty.dto.board;

import com.planty.entity.board.Board;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class AiMessageResDto {
    private Long id;
    private String content;
    private String sender;
    private LocalDateTime createdAt;
    private String aiImage;
    private List<BoardRecDto> recommendedBoards;
}
