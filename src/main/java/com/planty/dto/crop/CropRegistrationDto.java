package com.planty.dto.crop;

import com.planty.entity.crop.AnalysisStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class CropRegistrationDto {
    private Integer id;
    private String name;
    private LocalDate startAt;
    private LocalDate endAt;
    private String environment;
    private String temperature;
    private String height;
    private String howTo;
    private AnalysisStatus analysisStatus;
    private Boolean isRegistered;
    private String cropImg;
    // MultipartFile 필드 제거 - JSON 직렬화 문제 해결
}
