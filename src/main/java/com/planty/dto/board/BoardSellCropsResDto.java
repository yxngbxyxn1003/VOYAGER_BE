package com.planty.dto.board;

import com.planty.entity.crop.Crop;
import lombok.*;

import java.time.LocalDate;


// 판매 가능한 작물 리스트 (프론트 전달용)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardSellCropsResDto {
    private Integer cropId;
    private String name;
    private String cropImg;
    private LocalDate startAt;
    private LocalDate endAt;

    public static BoardSellCropsResDto of(Crop crop) {
        return BoardSellCropsResDto.builder()
                .cropId(crop.getId())
                .name(crop.getName())
                .cropImg(crop.getCropImg())
                .startAt(crop.getStartAt())
                .endAt(crop.getEndAt())
                .build();
    }
}
