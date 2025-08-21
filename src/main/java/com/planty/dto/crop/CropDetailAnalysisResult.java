package com.planty.dto.crop;

import com.planty.entity.crop.AnalysisType;
import lombok.Getter;
import lombok.Setter;

/**
 * 작물 세부 분석 결과 DTO
 */
@Getter @Setter
public class CropDetailAnalysisResult {
    private boolean success;
    private String message;
    private AnalysisType analysisType;
    
    // 현재상태 분석 결과
    private String currentStatusSummary;
    
    // 질병여부 분석 결과  
    private String diseaseStatus;
    private String diseaseDetails;
    private String preventionMethods;
    
    // 품질/시장성 분석 결과
    private String marketRatio;      // 출하시 상품 비율
    private String colorUniformity;  // 색 균일도
    private String saturation;       // 채도
    private String brightness;       // 명도
    private String tasteStorage;     // 맛과 저장성
    private String transportResistance; // 운송 저장 중 손상 저항성
    private String storageEvaluation;   // 저장성 평가
    
    public CropDetailAnalysisResult() {}
    
    public CropDetailAnalysisResult(boolean success, String message, AnalysisType analysisType) {
        this.success = success;
        this.message = message;
        this.analysisType = analysisType;
    }
}
