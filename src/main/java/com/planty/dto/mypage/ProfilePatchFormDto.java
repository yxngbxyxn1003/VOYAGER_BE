package com.planty.dto.mypage;

import lombok.Getter;
import lombok.Setter;


// 프론트에게 받는 프로필 수정 데이터
@Getter @Setter
public class ProfilePatchFormDto {
    private Integer id;
    private String nickname;
    private String profileImg;
}
