package com.planty.dto.crop;

import com.planty.entity.crop.AnalysisType;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

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
    
    /**
     * 분석 타입에 따른 분석 결과를 Map으로 반환
     */
    public Map<String, Object> getAnalysisResult() {
        Map<String, Object> result = new HashMap<>();
        
        if (analysisType == null) {
            return result;
        }
        
        switch (analysisType) {
            case CURRENT_STATUS:
                if (currentStatusSummary != null && !currentStatusSummary.trim().isEmpty()) {
                    result.put("currentStatusSummary", currentStatusSummary);
                }
                break;
                
            case DISEASE_CHECK:
                if (diseaseStatus != null && !diseaseStatus.trim().isEmpty()) {
                    result.put("diseaseStatus", diseaseStatus);
                }
                if (diseaseDetails != null && !diseaseDetails.trim().isEmpty()) {
                    result.put("diseaseDetails", diseaseDetails);
                }
                if (preventionMethods != null && !preventionMethods.trim().isEmpty()) {
                    result.put("preventionMethods", preventionMethods);
                }
                break;
                
            case QUALITY_MARKET:
                if (marketRatio != null && !marketRatio.trim().isEmpty()) {
                    result.put("marketRatio", marketRatio);
                }
                if (colorUniformity != null && !colorUniformity.trim().isEmpty()) {
                    result.put("colorUniformity", colorUniformity);
                }
                if (saturation != null && !saturation.trim().isEmpty()) {
                    result.put("saturation", saturation);
                }
                if (brightness != null && !brightness.trim().isEmpty()) {
                    result.put("brightness", brightness);
                }
                if (tasteStorage != null && !tasteStorage.trim().isEmpty()) {
                    result.put("tasteStorage", tasteStorage);
                }
                if (transportResistance != null && !transportResistance.trim().isEmpty()) {
                    result.put("transportResistance", transportResistance);
                }
                if (storageEvaluation != null && !storageEvaluation.trim().isEmpty()) {
                    result.put("storageEvaluation", storageEvaluation);
                }
                break;
                
        }
        
        return result;
    }
    
    /**
     * 분석 메시지를 반환
     */
    public String getAnalysisMessage() {
        return this.message;
    }
}
