package com.planty.dto.crop;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CropAnalysisResult {
    private String cropName;
    private String environment;
    private String temperature;
    private String height;
    private String howTo;
    private String analysisMessage;
    private boolean success;
    
    public CropAnalysisResult() {}
    
    public CropAnalysisResult(boolean success, String analysisMessage) {
        this.success = success;
        this.analysisMessage = analysisMessage;
    }
}
