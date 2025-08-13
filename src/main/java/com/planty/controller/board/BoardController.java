package com.planty.controller.board;

import com.planty.config.CustomUserDetails;
import com.planty.dto.board.BoardSellCropsDto;
import com.planty.service.board.BoardService;
import com.planty.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;


// 판매 게시판
@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final UserService userService;

    // 판매 가능한 작물 가져오기
    @GetMapping("/sell-crops")
    public ResponseEntity<List<BoardSellCropsDto>> getSellCrops(
            @AuthenticationPrincipal CustomUserDetails me
    ) {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 판매 가능한 작물 리스트 반환
        return ResponseEntity.ok(boardService.getSellCrops(me.getId()));
    }
}

