package com.planty.dto.board;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;


// 프론트 연결용 DTO
@Getter @Setter
public class BoardFormDto {
    @NotNull(message = "작물 ID가 필요해요.")
    private Integer cropId;

    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    @NotNull(message = "가격을 입력해주세요.")
    @Min(value = 0, message = "가격은 0 이상이어야 해요.")
    private Integer price;
}
