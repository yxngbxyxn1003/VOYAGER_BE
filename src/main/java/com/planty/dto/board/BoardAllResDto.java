package com.planty.dto.board;

import com.planty.entity.board.Board;
import com.planty.entity.board.BoardImage;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


// 프론트 전달용 판매 게시글 목록 데이터
@Getter @Setter
@Builder
public class BoardAllResDto {
    private Integer boardId;
    private String title;
    private Integer price;
    private String thumbnailImg;
    private String time;

    // 엔티티 -> DTO 반환
    public static BoardAllResDto of(Board board) {
        // 현재 시간과 게시글이 등록된 시간으로 텍스트 반환
        LocalDateTime dataTime = board.getCreatedAt();
        String time = toTimeAgo(dataTime);

        // 썸네일 이미지 찾기
        String thumbnailUrl = board.getImages().stream()
                .filter(BoardImage::getThumbnail) // thumbnail == true
                .findFirst()
                .map(BoardImage::getBoardImg)     // boardImg 값 추출
                .orElse(null);

        // 게시글 미리보기 데이터 반환
        return BoardAllResDto.builder()
                .boardId(board.getId())
                .title(board.getTitle())
                .price(board.getPrice())
                .time(time)
                .thumbnailImg(thumbnailUrl)
                .build();
    }

    // 시간 계산
    public static String toTimeAgo(LocalDateTime time) {
        LocalDateTime now = LocalDateTime.now();

        long years = time.until(now, ChronoUnit.YEARS);
        if (years > 0) return years + "년 전";

        long months = time.until(now, ChronoUnit.MONTHS);
        if (months > 0) return months + "달 전";

        long days = time.until(now, ChronoUnit.DAYS);
        if (days > 0) return days + "일 전";

        long hours = time.until(now, ChronoUnit.HOURS);
        if (hours > 0) return hours + "시간 전";

        long minutes = time.until(now, ChronoUnit.MINUTES);
        if (minutes > 0) return minutes + "분 전";

        return "방금 전";
    }
}
