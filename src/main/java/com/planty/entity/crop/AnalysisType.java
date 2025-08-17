package com.planty.entity.crop;

/**
 * 작물 분석 타입
 */
public enum AnalysisType {
    CURRENT_STATUS("현재상태 분석"),
    DISEASE_CHECK("질병 여부 분석"),  
    QUALITY_MARKET("품질/시장성 분석");
    
    private final String description;
    
    AnalysisType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
