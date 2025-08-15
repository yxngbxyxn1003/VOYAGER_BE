package com.planty.dto.diary;

import lombok.Builder;
import lombok.Getter;


// 재배일지 상세 페이지 응답 DTO
@Getter @Builder
public class DiaryDetailResDto {
    private DiaryDetailDto diary;
    private Boolean isOwner;
}
