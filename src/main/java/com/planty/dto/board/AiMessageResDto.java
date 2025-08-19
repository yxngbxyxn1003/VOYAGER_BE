package com.planty.dto.board;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AiMessageResDto {
    private Long id;
    private String content;
    private String sender;
    private LocalDateTime createdAt;
    private String aiImage;
}
