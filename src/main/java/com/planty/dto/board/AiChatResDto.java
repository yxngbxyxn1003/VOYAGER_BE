package com.planty.dto.board;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class AiChatResDto {
    private Long id;
    private LocalDateTime createdAt;
    private List<AiMessageResDto> messages;

}
