package com.planty.dto.crop;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.planty.entity.crop.AnalysisStatus;
import com.planty.entity.crop.Crop;
import com.planty.entity.crop.CropCategory;
import lombok.Builder;
import io.reactivex.annotations.BackpressureSupport;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeCropDto {
    private Integer id;
    private String name;
    private String cropImg;
    private LocalDateTime plantingDate;
    private LocalDateTime endAt;

    private Boolean isRegistered;
    private AnalysisStatus analysisStatus;
    private List<CropCategory> cropCategory;

    public static HomeCropDto of(Crop crop) {

        return HomeCropDto.builder()
                .id(crop.getId())
                .name(crop.getName())
                .cropImg(crop.getCropImg())
                .plantingDate(crop.getStartAt())
                .endAt(crop.getEndAt())
                .isRegistered(crop.getIsRegistered())
                .analysisStatus(crop.getAnalysisStatus())
                .cropCategory(crop.getCategories())
                .build();
    }
}


