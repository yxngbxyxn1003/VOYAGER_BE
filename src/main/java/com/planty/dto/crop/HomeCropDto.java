package com.planty.dto.crop;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeCropDto {
    private Integer id;
    private String name;
    private String cropImg;
    private String plantingDate;
    private Boolean isRegistered;
    private String analysisStatus;
    private String cropCategory;
}
