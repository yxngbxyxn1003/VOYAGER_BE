package com.planty.dto.chat;

import com.planty.entity.chat.Chat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// ChatDTO.java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatDto {
    private Long chatId;
    private Integer sellerId;

    public ChatDto(Chat chat) {
    }
}

