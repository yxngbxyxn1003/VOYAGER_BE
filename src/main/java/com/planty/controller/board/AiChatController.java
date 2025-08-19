package com.planty.controller.board;

import com.planty.dto.board.AiChatResDto;
import com.planty.dto.board.AiMessageResDto;
import com.planty.entity.board.AiChat;
import com.planty.entity.board.AiMessage;
import com.planty.entity.user.User;
import com.planty.repository.user.UserRepository;
import com.planty.service.board.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/aichat")
public class AiChatController {

    private final AiChatService aiChatService;
    private final UserRepository userRepository;

    // 새로운 채팅 시작
    @PostMapping("/start")
    public ResponseEntity<AiChat> startChat(@AuthenticationPrincipal Long userId) {
        User user = userRepository.findById(Math.toIntExact(userId))
                .orElseThrow(() -> new RuntimeException("유저 없음"));
        AiChat chat = aiChatService.createChat(user);
        return ResponseEntity.ok(chat);
    }

    // 메시지 전송 + AI 응답 반환
    @PostMapping("/{chatId}/send")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @PathVariable Long chatId,
            @RequestParam String content
    ) {
        AiChat chat = aiChatService.getChat(chatId);
        AiMessage userMsg = aiChatService.saveUserMessage(chat, content);
        AiMessage aiMsg = aiChatService.generateAiResponse(chat, content);

        Map<String, Object> response = new HashMap<>();
        response.put("userMessage", new AiMessageResDto(
                userMsg.getId(), userMsg.getContent(), userMsg.getSender(), userMsg.getCreatedAt(), userMsg.getAiImage()
        ));
        response.put("aiMessage", new AiMessageResDto(
                aiMsg.getId(), aiMsg.getContent(), aiMsg.getSender(), aiMsg.getCreatedAt(), aiMsg.getAiImage()
        ));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<AiChatResDto> getChatMessages(@PathVariable Long chatId) {
        AiChat chat = aiChatService.getChat(chatId);

        List<AiMessageResDto> messages = chat.getMessages().stream()
                .map(msg -> new AiMessageResDto(
                        msg.getId(),
                        msg.getContent(),
                        msg.getSender(),
                        msg.getCreatedAt(),
                        msg.getAiImage()
                ))
                .toList();

        AiChatResDto chatResponse = new AiChatResDto(chat.getId(), chat.getCreatedAt(), messages);
        return ResponseEntity.ok(chatResponse);
    }


}
