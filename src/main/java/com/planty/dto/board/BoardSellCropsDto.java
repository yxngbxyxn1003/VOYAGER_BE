package com.planty.dto.board;

import com.planty.entity.crop.Crop;
import lombok.*;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;


// 판매 가능한 작물 리스트 (프론트 전달용)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardSellCropsDto {
    private Integer cropId;
    private String name;
    private String cropImg;
    private LocalDate startAt;
    private LocalDate endAt;

    public static BoardSellCropsDto of(Crop crop) {
        return BoardSellCropsDto.builder()
                .cropId(crop.getId())
                .name(crop.getName())
                .cropImg(crop.getCropImg())
                .startAt(crop.getStartAt())
                .endAt(crop.getEndAt())
                .build();
    }
}
