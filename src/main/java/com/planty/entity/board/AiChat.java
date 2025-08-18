package com.planty.entity.board;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.planty.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"messages"})
public class AiChat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @OneToMany(mappedBy = "aiChat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AiMessage> messages = new ArrayList<>();

    private LocalDateTime createdAt;
}
