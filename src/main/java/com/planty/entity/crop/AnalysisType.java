package com.planty.entity.crop;

/**
 * 작물 분석 타입
 * - REGISTRATION_ANALYSIS: 작물 등록 시 재배방법 분석
 * - DIAGNOSIS_ANALYSIS: 작물 진단 시 태그별 분석
 */
public enum AnalysisType {
    // 재배방법 분석 (작물 등록 시)
    REGISTRATION_ANALYSIS("재배방법 분석"),
    
    // 태그별 진단 분석 (작물 진단 시)
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
    
    /**
     * 재배방법 분석인지 확인
     */
    public boolean isRegistrationAnalysis() {
        return this == REGISTRATION_ANALYSIS;
    }
    
    /**
     * 진단 분석인지 확인
     */
    public boolean isDiagnosisAnalysis() {
        return this == CURRENT_STATUS || this == DISEASE_CHECK || this == QUALITY_MARKET;
    }
}
