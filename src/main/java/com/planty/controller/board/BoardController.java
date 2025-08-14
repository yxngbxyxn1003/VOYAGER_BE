package com.planty.controller.board;

import com.planty.common.ApiSuccess;
import com.planty.config.CustomUserDetails;
import com.planty.dto.board.BoardDetailResDto;
import com.planty.dto.board.BoardFormDto;
import com.planty.dto.board.BoardSaveFormDto;
import com.planty.dto.board.BoardSellCropsDto;
import com.planty.entity.board.Board;
import com.planty.entity.board.BoardImage;
import com.planty.repository.board.BoardRepository;
import com.planty.repository.crop.CropRepository;
import com.planty.repository.user.UserRepository;
import com.planty.service.board.BoardService;
import com.planty.service.user.UserService;
import com.planty.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


// 판매 게시판
@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final StorageService storageService;

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

    // 판매 게시글 등록 (JSON+파일)
    @PostMapping(value="/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createBoard(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestPart("form") @Validated BoardFormDto form,  // 판매 게시글 데이터
            @RequestPart(value = "images", required = false) List<MultipartFile> images // 판매 게시글 이미지
    ) throws IOException {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 파일 저장 → URL 리스트 생성
        List<String> urls = new ArrayList<>();
        if (images != null) {
            for (MultipartFile f : images) {
                if (!f.isEmpty()) {
                    urls.add(storageService.save(f, "board"));
                }
            }
        }

        // BoardSaveFormDto로 변환
        BoardSaveFormDto dto = new BoardSaveFormDto();
        dto.setCropId(form.getCropId());
        dto.setTitle(form.getTitle());
        dto.setContent(form.getContent());
        dto.setPrice(form.getPrice());
        dto.setImageUrls(urls);

        // 서비스 호출
        boardService.saveBoard(me.getId(), dto);

        // 컨벤션: 데이터 없을 때 status + message
        return ResponseEntity.status(201).body(new ApiSuccess(201, "성공적으로 처리되었습니다."));
    }

    // 판매 게시글 상세 조회
    @GetMapping("/details/{id}")
    public ResponseEntity<BoardDetailResDto> getBoardDetail(
            @AuthenticationPrincipal CustomUserDetails me,

            // 게시글 id
            @PathVariable Integer id
    ) {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 판매 게시글 데이터 가져오기
        BoardDetailResDto dto = boardService.getBoardDetail(id, me);

        // 판매 게시글 데이터 반환
        return ResponseEntity.ok(dto);
    }
}

