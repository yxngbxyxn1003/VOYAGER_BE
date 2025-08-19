package com.planty.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// ChatMessageDTO.java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long id;
    private Long chatId;
    private Integer senderId;
    private String content;
    private Boolean read;
    private String chatImg;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
}
