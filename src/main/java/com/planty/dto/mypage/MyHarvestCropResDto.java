package com.planty.dto.mypage;

import com.planty.entity.crop.Crop;
import lombok.Builder;
import lombok.Getter;


// 내 재배 완료된 작물
@Getter @Builder
public class MyHarvestCropResDto {
    private Integer cropId;
    private String name;
    private String thumbnail;
    private Boolean harvest;

    // 엔티티 -> DTO 반환
    public static MyHarvestCropResDto of(Crop crop) {
        // 썸네일 이미지 찾기
        String thumbnailUrl = crop.getCropImg();

        // 내 재배 완료된 작물 데이터 반환
        return MyHarvestCropResDto.builder()
                .cropId(crop.getId())
                .name(crop.getName())
                .thumbnail(thumbnailUrl)
                .harvest(crop.getHarvest())
                .build();
    }
}
