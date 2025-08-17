package com.planty.controller.board;

import com.planty.entity.board.AiChat;
import com.planty.entity.board.AiMessage;
import com.planty.entity.user.User;
import com.planty.repository.user.UserRepository;
import com.planty.service.board.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/aichat")
public class AiChatController {

    private final AiChatService aiChatService;
    private final UserRepository userRepository;

    // 새로운 채팅 시작
    @PostMapping("/start/{userId}")
    public ResponseEntity<AiChat> startChat(@PathVariable Long userId) {
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
        response.put("userMessage", userMsg);
        response.put("aiMessage", aiMsg);

        return ResponseEntity.ok(response);
    }
}
