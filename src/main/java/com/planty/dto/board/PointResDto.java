package com.planty.dto.board;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

// 프론트에 전달할 포인트
@Getter @Setter @Builder
public class PointResDto {
    private Integer id;
    private Integer point;
}
