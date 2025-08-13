package com.planty.dto.board;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


// 서비스 저장용 중간 데이터
@Getter @Setter
public class BoardSaveFormDto {
    private Integer cropId;
    private String title;
    private String content;
    private Integer price;
    private List<String> imageUrls = new ArrayList<>();
}
