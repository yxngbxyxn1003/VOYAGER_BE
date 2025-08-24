package com.planty.dto.board;

import com.planty.entity.diary.Diary;
import com.planty.entity.diary.DiaryImage;
import lombok.Builder;
import lombok.Getter;


// 판매 게시글 별 재배 일지
@Getter @Builder
public class BoardDiaryResDto {
    private Integer diaryId;
    private String title;
    private String content;
    private String thumbnailImg;

    // 엔티티 -> DTO 반환
    public static BoardDiaryResDto of(Diary diary) {

        // 미리보기 한 줄 추출
        String content = (diary.getContent()).split("\\.")[0]+".";

        // 썸네일 이미지 찾기
        String thumbnailUrl = diary.getImages().stream()
                .filter(DiaryImage::getThumbnail) // thumbnail == true
                .findFirst()
                .map(DiaryImage::getDiaryImg)     // boardImg 값 추출
                .orElse(null);

        // 게시글 미리보기 데이터 반환
        return BoardDiaryResDto.builder()
                .diaryId(diary.getId())
                .title(diary.getTitle())
                .content(content)
                .thumbnailImg(thumbnailUrl)
                .build();
    }
}
