package com.planty.dto.mypage;

import lombok.Getter;
import lombok.Setter;


// 비밀번호 변경
@Getter @Setter
public class PasswordFormDto {
    private String oldPassword;
    private String newPassword;
}
