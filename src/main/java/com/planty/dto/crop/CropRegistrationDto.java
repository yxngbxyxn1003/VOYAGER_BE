package com.planty.dto.crop;

import com.planty.entity.crop.AnalysisStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

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
    private MultipartFile imageFile; // 업로드할 이미지 파일
}
