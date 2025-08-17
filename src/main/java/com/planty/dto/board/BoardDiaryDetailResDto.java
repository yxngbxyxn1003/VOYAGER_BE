package com.planty.dto.board;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


// 프론트 전달용 DTO
@Getter @Setter
@Builder
public class BoardDiaryDetailResDto {
    private Integer diaryId;
    private String title;
    private String content;
    private List<String> images;
    private String time;
    private Boolean isOwner;
}
