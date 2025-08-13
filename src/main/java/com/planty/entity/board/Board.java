package com.planty.entity.board;

import com.planty.entity.crop.Crop;
import com.planty.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


// 판매 게시글 엔티티
@Entity
@Table(name = "board")
@Getter @Setter
public class Board {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crop_id", unique = true, nullable = false)
    private Crop crop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id")
    private User user;

    private String title;

    @Lob
    private String content;
    private Integer price;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoardImage> images = new ArrayList<>();

    @Column(nullable = false)
    private Boolean sell = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime modifiedAt;
}
