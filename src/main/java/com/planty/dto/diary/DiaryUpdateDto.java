package com.planty.dto.diary;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@Getter @Setter
public class DiaryUpdateDto {
    
    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 100, message = "제목은 100자 이내로 입력해주세요.")
    private String title;
    
    @NotBlank(message = "내용을 입력해주세요.")
    @Size(max = 5000, message = "내용은 5000자 이내로 입력해주세요.")
    private String content;
    
    // AI 분석 결과 포함 여부
    private Boolean includeAnalysis;
    
    // 직접 입력한 분석 내용
    private String analysis;
    
    // AI 진단 결과 포함 여부
    private Boolean includeDiagnosis;
    
    // 진단 타입 (선택적)
    private String diagnosisType;
    
    // 진단 데이터 (선택적)
    private String diagnosisData;
    
    // 기존 이미지 URL 목록 (유지할 이미지들)
    private List<String> existingImages;
    
    // 삭제할 이미지 URL 목록
    private List<String> imagesToDelete;
}
