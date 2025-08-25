package com.planty.dto.diary;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;


// 재배일지 상세 정보 DTO
@Getter @Builder
public class DiaryDetailDto {
    private Integer diaryId;
    private Integer cropId;
    private String cropName;
    private String title;
    private String content;
    private String analysis;
    private List<String> images;
    private String thumbnailImage; // 썸네일 이미지 URL
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    
    // 재배일지 타입 정보
    private String diaryType; // "CROP_BASED", "DIAGNOSIS_BASED", "GENERAL"
    private String diagnosisType; // 진단 타입 (진단결과 기반인 경우)
    private Boolean includeDiagnosis; // 진단 결과 포함 여부
}
