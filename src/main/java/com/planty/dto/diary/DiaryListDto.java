package com.planty.dto.diary;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;


// 재배일지 목록용 DTO
@Getter @Builder
public class DiaryListDto {
    private Integer diaryId;
    private String title;
    private String cropName;
    private String thumbnailImage;
    private LocalDateTime createdAt;
    
    // 재배일지 타입 정보
    private String diaryType; // "CROP_BASED", "DIAGNOSIS_BASED", "GENERAL"
    private String diagnosisType; // 진단 타입 (진단결과 기반인 경우)
}
