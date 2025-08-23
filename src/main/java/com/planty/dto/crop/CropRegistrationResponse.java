package com.planty.dto.crop;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CropRegistrationResponse {
    private boolean success;
    private String message;
    private String analysisType;
    private CropRegistrationDto cropData;
    private Map<String, Object> analysisResult;
    private Integer tempCropId;
}
