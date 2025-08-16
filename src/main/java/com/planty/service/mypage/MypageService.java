package com.planty.service.mypage;


import com.planty.dto.mypage.MySellBoardResDto;
import com.planty.dto.mypage.ProfileResDto;
import com.planty.entity.board.Board;
import com.planty.entity.user.User;
import com.planty.repository.board.BoardRepository;
import com.planty.repository.mypage.MypageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// 마이페이지
@Service
@Transactional
@RequiredArgsConstructor
public class MypageService {

    private final MypageRepository mypageRepository;
    private final BoardRepository boardRepository;

    // 프로필 정보 불러오기
    public ProfileResDto getProfile(Integer userId) {
        // 유저 확인
        User user = mypageRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음: " + userId));

        return ProfileResDto.builder()
                .id(user.getId())
                .name(user.getNickname())
                .point(user.getPoint())
                .profileImg(user.getProfileImg())
                .build();
    }

    // 내가 쓴 판매 게시글 불러오기
    public List<MySellBoardResDto> getMySellBoard(Integer userId) {
        // 유저 확인
        User user = mypageRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음: " + userId));

        // 내가 쓴 판매 게시글 중 판매 중인 게시글
        List<Board> boards = boardRepository.findMyBoardsOrderByStatusAndCreated(userId);

        // DTO로 변환
        return boards.stream()
                .map(MySellBoardResDto::of)
                .toList();
    }
}
