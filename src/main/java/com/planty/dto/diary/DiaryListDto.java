package com.planty.dto.diary;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;


// 재배일지 목록용 DTO
@Getter @Builder
public class DiaryListDto {
    private Integer diaryId;
    private String title;
    private String cropName;
    private String thumbnailImage;
    private LocalDateTime createdAt;
}
