package com.planty.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import org.springframework.stereotype.Component;
import com.planty.service.board.AiChatService;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final AiChatService aiChatService;

    @Autowired
    public ChatWebSocketHandler(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String userMessage = message.getPayload();

        // 🔹 GPT-4.1 호출
        String aiResponse = aiChatService.callOpenAi(userMessage);

        // 🔹 클라이언트에 다시 전송
        session.sendMessage(new TextMessage(aiResponse));
    }
}
