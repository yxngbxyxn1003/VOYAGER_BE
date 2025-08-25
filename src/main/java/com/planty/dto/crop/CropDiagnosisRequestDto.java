package com.planty.dto.crop;

import com.planty.entity.crop.AnalysisType;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;

/**
 * 작물 진단 요청 DTO
 */
@Getter @Setter
public class CropDiagnosisRequestDto {
    
    @NotNull(message = "작물 ID가 필요합니다.")
    private Integer cropId;
    
    @NotNull(message = "진단 타입이 필요합니다.")
    private AnalysisType analysisType;
    
    /**
     * 진단할 이미지 URL (선택적 - 작물 기본 이미지 대신 다른 이미지 사용시)
     */
    private String image;
}
