package com.planty.dto.board;

import lombok.Builder;
import lombok.Getter;


// 판매 페이지 -> 판매자 정보
@Getter @Builder
public class SellerDto {
    private Integer sellerId;
    private String sellerName;
    private String profileImg;
}
