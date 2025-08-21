package com.planty.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDto {
    private Long chatId;
    private List<ParticipantDto> participants;
    private ChatMessageDto lastMessage;
    private Integer countMessages;
}
