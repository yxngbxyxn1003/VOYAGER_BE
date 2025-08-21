package com.planty.dto.mypage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;


// 프론트에게 받는 프로필 수정 데이터
@Getter @Setter
public class ProfilePatchFormDto {
    private String nickname;           // 텍스트 필드
    private MultipartFile profileImg;  // 파일 필드(선택)
}
