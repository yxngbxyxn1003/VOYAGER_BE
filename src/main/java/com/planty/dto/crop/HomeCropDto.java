package com.planty.dto.crop;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeCropDto {
    @JsonProperty("id")
    private Integer id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("cropImg")
    private String cropImg;
    
    @JsonProperty("plantingDate")
    private String plantingDate;

    @JsonProperty("endAt")
    private String endAt;
    
    @JsonProperty("isRegistered")
    private Boolean isRegistered;
    
    @JsonProperty("analysisStatus")
    private String analysisStatus;
    
    @JsonProperty("cropCategory")
    private String cropCategory;
}
