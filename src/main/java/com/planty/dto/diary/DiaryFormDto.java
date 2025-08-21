package com.planty.dto.diary;

import com.planty.entity.crop.AnalysisType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;


// 재배일지 작성 폼 DTO
@Getter @Setter
public class DiaryFormDto {
    @NotNull(message = "작물 ID가 필요합니다.")
    private Integer cropId;

    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    /**
     * AI 분석 결과 (직접 입력한 경우)
     */
    private String analysis;
    
    /**
     * AI 진단 결과 포함 여부 (기본값: false)
     */
    private Boolean includeAnalysis = false;

    /**
     * 포함할 진단 타입 (진단 결과를 포함하는 경우)
     */
    private AnalysisType diagnosisType;

    /**
     * 진단 결과 데이터 (JSON 형태로 저장)
     */
    private String diagnosisData;
}
