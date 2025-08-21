package com.planty.controller.board;

import com.planty.config.CustomUserDetails;
import com.planty.dto.board.AiChatResDto;
import com.planty.dto.board.AiMessageResDto;
import com.planty.dto.board.AiMessageWithBoardsDto;
import com.planty.entity.board.AiChat;
import com.planty.entity.board.AiMessage;
import com.planty.entity.board.Board;
import com.planty.entity.user.User;
import com.planty.repository.user.UserRepository;
import com.planty.service.board.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
    public ResponseEntity<AiChat> startChat(@AuthenticationPrincipal CustomUserDetails authUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("hereis" + authentication.getPrincipal());
        User user = userRepository.findById(authUser.getId())
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

        // 1. 사용자 메시지 저장
        AiMessage userMsg = aiChatService.saveUserMessage(chat, content);

        Map<String, Object> response = new HashMap<>();

        // 2. AI 메시지 생성
        if (content.contains("구매")) {
            AiMessage aiMsg = aiChatService.generateAiResponse(chat, content);

            // 게시글 추천 추가
            List<Board> recommendedBoards = aiChatService.getRecommendedBoards(content);
            response.put("aiMessage", new AiMessageResDto(
                    aiMsg.getId(),
                    aiMsg.getContent(),
                    "ai",
                    aiMsg.getCreatedAt(),
                    aiMsg.getAiImage(),
                    recommendedBoards
            ));
        } else {
            AiMessage aiMsg = aiChatService.generateAiResponse(chat, content);
            response.put("aiMessage", new AiMessageResDto(
                    aiMsg.getId(),
                    aiMsg.getContent(),
                    aiMsg.getSender(),
                    aiMsg.getCreatedAt(),
                    aiMsg.getAiImage(),
                    null
            ));
        }

        // 3. 사용자 메시지도 응답에 담을 수 있음
        response.put("userMessage", new AiMessageResDto(
                userMsg.getId(),
                userMsg.getContent(),
                userMsg.getSender(),
                userMsg.getCreatedAt(),
                userMsg.getAiImage(),
                null
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
                        msg.getAiImage(),
                        msg.getSender().equals("ai") && msg.getContent().contains("구매")
                                ? aiChatService.getRecommendedBoards(msg.getContent())
                                : null
                ))
                .toList();

        AiChatResDto chatResponse = new AiChatResDto(chat.getId(), chat.getCreatedAt(), messages);
        return ResponseEntity.ok(chatResponse);
    }


//    @GetMapping("/{chatId}")
//    public ResponseEntity<AiChatResDto> getChatMessages(@PathVariable Long chatId) {
//        AiChat chat = aiChatService.getChat(chatId);
//
//        List<AiMessageResDto> messages = chat.getMessages().stream().map(msg -> {
//            if ("ai".equals(msg.getSender()) && msg.getContent().contains("구매")) {
//                AiMessageWithBoardsDto msgWithBoards = aiChatService.generateAiResponseWithBoards(chat, msg.getContent());
//                return new AiMessageResDto(
//                        msgWithBoards.getId(),
//                        msgWithBoards.getContent(),
//                        "ai",
//                        LocalDateTime.now(),
//                        null,
//                        msgWithBoards.getRecommendedBoards()
//                );
//            } else {
//                return new AiMessageResDto(
//                        msg.getId(),
//                        msg.getContent(),
//                        msg.getSender(),
//                        msg.getCreatedAt(),
//                        msg.getAiImage(),
//                        null
//                );
//            }
//        }).toList();
//
//        AiChatResDto chatResponse = new AiChatResDto(chat.getId(), chat.getCreatedAt(), messages);
//        return ResponseEntity.ok(chatResponse);
//    }



}
