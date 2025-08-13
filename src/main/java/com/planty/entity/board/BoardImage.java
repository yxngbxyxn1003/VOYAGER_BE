package com.planty.entity.board;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;


// 판매 게시글 이미지
@Entity
@Table(name = "board_image")
@Getter @Setter
public class BoardImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 다대일 (여러 이미지가 하나의 글에)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(nullable = false, length = 500)
    private String boardImg;            // 또는 path

    @Column(nullable = false)
    private Boolean thumbnail = false; // 대표 이미지 여부

    @CreationTimestamp
    private LocalDateTime createdAt;
}

