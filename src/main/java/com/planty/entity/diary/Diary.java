package com.planty.entity.diary;

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


// 재배일지 엔티티
@Entity
@Table(name = "diary")
@Getter @Setter
public class Diary {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "crop_id", nullable = true)
    private Crop crop;

    @Lob
    private String analysis; // AI 분석 결과

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    /**
     * AI 진단 결과 포함 여부
     */
    @Column(name = "include_diagnosis")
    private Boolean includeDiagnosis = false;

    /**
     * 포함된 진단 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "diagnosis_type")
    private com.planty.entity.crop.AnalysisType diagnosisType;

    /**
     * 진단 결과 데이터 (JSON 형태로 저장)
     */
    @Lob
    @Column(name = "diagnosis_data")
    private String diagnosisData;

    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiaryImage> images = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime modifiedAt;
}
