package com.planty.dto.diary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;


// 재배일지 작성 폼 DTO
@Getter @Setter
public class DiaryFormDto {
    @NotNull(message = "작물 ID가 필요합니다.")
    private Integer cropId;

    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    private String analysis; // AI 분석 결과 (옵션)
}
