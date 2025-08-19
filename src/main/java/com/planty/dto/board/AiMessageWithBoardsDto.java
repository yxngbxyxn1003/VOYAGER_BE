package com.planty.dto.board;

import com.planty.entity.board.Board;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class AiMessageWithBoardsDto{
    Long id;
    String content;
    List<Board> recommendedBoards;
}
