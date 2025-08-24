package com.planty.service.board;

import com.planty.dto.board.AiChatDto;
import com.planty.dto.board.AiMessageResDto;
import com.planty.dto.board.AiMessageWithBoardsDto;
import com.planty.entity.board.AiChat;
import com.planty.entity.board.AiMessage;
import com.planty.entity.board.Board;
import com.planty.entity.user.User;
import com.planty.repository.board.AiChatRepository;
import com.planty.repository.board.AiMessageRepository;
import com.planty.repository.board.BoardRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import org.springframework.web.reactive.function.client.WebClient;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiChatService {

    private final AiChatRepository aiChatRepository;
    private final AiMessageRepository aiMessageRepository;
    private final BoardRepository boardRepository;

    // WebClient 초기화
    private final WebClient webClient;

    @Value("${openai.api.key}")
    private String apiKey;

    @Transactional
    public AiChatDto createChat(User user) {
        // 이미 존재하는 채팅 조회
        Optional<AiChat> existingChat = aiChatRepository.findByUser(user);

        AiChat chat;
        if (existingChat.isPresent()) {
            chat = existingChat.get(); // 이미 존재하면 기존 채팅 반환
        } else {
            // 존재하지 않으면 새로 생성
            chat = AiChat.builder()
                    .user(user)
                    .createdAt(LocalDateTime.now())
                    .build();
            chat = aiChatRepository.save(chat);
        }

        return new AiChatDto(chat.getId());
    }



    // 유저 메시지 저장
    public AiMessage saveUserMessage(AiChat chat, String content) {
        AiMessage message = AiMessage.builder()
                .aiChat(chat)
                .content(content)
                .sender("user")
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build();
        return aiMessageRepository.save(message);
    }

    // AI 응답 생성 (GPT-4.1 호출)
    public AiMessage generateAiResponse(AiChat chat, String userMessage) {
        String aiReply = callOpenAi(userMessage); // GPT-4.1 호출

        AiMessage aiMessage = AiMessage.builder()
                .aiChat(chat)
                .content(aiReply)
                .sender("ai")
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build();

        return aiMessageRepository.save(aiMessage);
    }

    // 판매게시글 추천만 가져오기
    public List<Board> getRecommendedBoards(String keyword) {
        return boardRepository.findByCropCategoryName(keyword)
                .stream()
                .limit(5) // 최대 5개
                .toList();
    }


    public AiChat getChat(Long chatId) {
        return aiChatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("채팅 없음"));
    }

    // WebClient로 OpenAI GPT-4.1 호출
    @SuppressWarnings("unchecked")
    public String callOpenAi(String userMessage) {
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4.1",
                "messages", List.of(
                        Map.of("role", "system", "content", "너는 친절한 농업 조수야. 500자 이내로 답변해."),
                        Map.of("role", "user", "content", userMessage)
                ),
                "max_tokens", 500,
                "temperature", 0.7
        );

        Map<String, Object> response = webClient.post()
                .uri("https://api.openai.com/v1/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}
