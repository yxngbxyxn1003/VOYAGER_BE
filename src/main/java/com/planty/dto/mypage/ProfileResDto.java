package com.planty.dto.mypage;

import lombok.Builder;
import lombok.Getter;


// 프론트 전달용 유저 프로필
@Getter @Builder
public class ProfileResDto {
    private Integer id;
    private String name;
    private Integer point;
    private String profileImg;
}
