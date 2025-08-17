package com.planty.entity.board;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aiChatId", nullable = false)
    private AiChat aiChat;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String sender; // "user" or "ai"

    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    private String aiImage; // 선택적 이미지
}
