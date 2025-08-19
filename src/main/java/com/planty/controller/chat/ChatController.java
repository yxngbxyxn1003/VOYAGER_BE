package com.planty.controller.chat;

import com.planty.config.CustomUserDetails;
import com.planty.dto.chat.ChatDto;
import com.planty.dto.chat.ChatMessageDto;
import com.planty.dto.chat.ChatRoomDto;
import com.planty.dto.chat.SendMessageRequest;
import com.planty.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.attribute.UserPrincipal;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // 채팅 시작
    @PostMapping("/start")
    public ResponseEntity<ChatDto> startChat(@RequestBody List<Integer> userIds) {
        return ResponseEntity.ok(chatService.startChat(userIds));
    }

    // 채팅 기록 조회
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getMessages(@PathVariable Long chatId) {
        return ResponseEntity.ok(chatService.getChatMessages(chatId));
    }

    // 발신/수신 메시지 조회
    @GetMapping("/{chatId}/messages/sender/{senderId}")
    public ResponseEntity<List<ChatMessageDto>> getMessagesBySender(
            @PathVariable Long chatId,
            @PathVariable Integer senderId) {
        return ResponseEntity.ok(chatService.getMessagesBySender(chatId, senderId));
    }

    // 메시지 전송
    @PostMapping("/{chatId}/send")
    public ResponseEntity<ChatMessageDto> sendMessage(
            @PathVariable Long chatId,
            @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(chatService.sendMessage(chatId, request.getSenderId(), request.getContent()));
    }

    @GetMapping("/me")
    public ResponseEntity<List<ChatRoomDto>> getChat(@AuthenticationPrincipal Integer userId) {
        return ResponseEntity.ok(chatService.getMyChatRooms(userId));
    }

    @DeleteMapping("/delete/{chatId}")
    public ResponseEntity<Void> deleteChatRoom(@PathVariable Long chatId, @AuthenticationPrincipal Integer userId) {
        chatService.deleteChatRoom(chatId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/block/{blockId}")
    public ResponseEntity<Void> blockUser(@AuthenticationPrincipal  Integer userId, @PathVariable Integer blockId) {
        chatService.blockUser(blockId, userId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read/{chatId}")
    public ResponseEntity<Void> readChat(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long chatId) {
        chatService.readChat(chatId, user.getId());
        return ResponseEntity.ok().build();
    }
}

