package com.planty.dto.board;

import lombok.Builder;
import lombok.Getter;
import java.util.List;


// 판매 페이지 -> 게시글 정보
@Getter @Builder
public class BoardDetailDto {
    private Integer boardId;
    private Integer cropId;
    private String title;
    private String content;
    private Integer price;
    private Boolean sell;
    private List<String> images;
}

