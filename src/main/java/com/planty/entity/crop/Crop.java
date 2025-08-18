package com.planty.entity.crop;

import com.planty.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


// 작물 엔티티
@Entity
@Table(name = "crop")
@Getter @Setter
@ToString
public class Crop {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id") // FK: crop.user_id -> users.id
    private User user;

    private String name;
    private String cropImg;

    private LocalDate startAt;
    private LocalDate endAt;

    private String environment;
    private String temperature;
    private String height;
    private String howTo;

    // 분석 상태 관리
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
    //TODO: 테스트 오류로 인한 임시 수정
    @Transient
    private AnalysisStatus analysisStatus = AnalysisStatus.PENDING;

    // 작물 등록 상태 (이미지 업로드 후 분석 완료되기 전까지는 false)
//    @Column(nullable = false)
    //TODO: 테스트 오류로 인한 임시 수정
    @Transient
    private Boolean isRegistered = false;

    private Boolean harvest;

    @OneToMany(mappedBy = "crop", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CropCategory> categories = new ArrayList<>();


    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime modifiedAt;
}
