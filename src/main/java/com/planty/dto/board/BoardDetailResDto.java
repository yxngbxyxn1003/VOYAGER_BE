package com.planty.dto.board;

import lombok.Builder;
import lombok.Getter;


// 판매 페이지 게시글 프론트 전달용
@Getter @Builder
public class BoardDetailResDto {
    private BoardDetailDto board;
    private SellerDto seller;
    private Boolean isOwner;
}
