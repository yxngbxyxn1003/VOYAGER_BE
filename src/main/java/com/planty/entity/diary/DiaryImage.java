package com.planty.entity.diary;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


// 재배일지 이미지 엔티티
@Entity
@Table(name = "diary_image")
@Getter @Setter
public class DiaryImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @Column(nullable = false, length = 500)
    private String diaryImg;

    @Column(nullable = false)
    private Boolean thumbnail = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
