package com.planty.controller.board;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planty.common.ApiSuccess;
import com.planty.config.CustomUserDetails;
import com.planty.dto.board.*;
import com.planty.service.board.BoardService;
import com.planty.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


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
    @PostMapping(value="/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
        BoardDetailResDto dto = boardService.getBoardDetail(id, me.getId());

        // 판매 게시글 데이터 반환
        return ResponseEntity.ok(dto);
    }

    // 판매 게시글 수정 (JSON + 파일)
    @PutMapping(value="/details/{id:\\d+}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateBoard(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable Integer id,

            // form 파트는 그대로 DTO 바인딩 가능 (application/json로 보내면 스프링이 매핑해줌)
            @RequestPart("form") @Validated BoardFormDto form,

            // imageUrls는 JSON 배열로 받기 → String으로 받고 직접 파싱
            @RequestPart(value = "imageUrls", required = false) String imageUrlsJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images

    ) throws IOException {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // (1) 새 파일 업로드 → URL 생성
        List<String> newUrls = new ArrayList<>();
        if (images != null) {
            for (MultipartFile f : images) {
                if (!f.isEmpty()) newUrls.add(storageService.save(f, "board"));
            }
        }

        // (2) JSON 배열 파싱 (null/빈문자열 방어)
        List<String> keepImageUrls = null;
        if (imageUrlsJson != null && !imageUrlsJson.isBlank()) {
            keepImageUrls = new ObjectMapper().readValue(
                    imageUrlsJson, new TypeReference<List<String>>() {});
        }

        // (3) 서비스 DTO 구성 (새로 추가된 것만)
        BoardSaveFormDto dto = new BoardSaveFormDto();
        dto.setCropId(form.getCropId());
        dto.setTitle(form.getTitle());
        dto.setContent(form.getContent());
        dto.setPrice(form.getPrice());
        dto.setImageUrls(newUrls);

        // (4) 업데이트
        boardService.updateBoard(id, me.getId(), dto, keepImageUrls);

        // 성공 json 반환
        return ResponseEntity.ok(new ApiSuccess(200, "성공적으로 처리되었습니다."));
    }

    // 판매 게시글 판매 상태 수정
    @PatchMapping(value="/details/{id:\\d+}")
    public ResponseEntity<?> updateSellStatus(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable Integer id,
            @RequestBody SellStatusFormDto sellStatusFormDto
    ) throws IOException {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 판매 상태 업데이트
        boardService.updateSellStatus(id, me.getId(), sellStatusFormDto.getSellStatus());

        // 성공 json 반환
        return ResponseEntity.ok(new ApiSuccess(200, "성공적으로 처리되었습니다."));
    }

    // 판매 게시글 삭제
    @DeleteMapping(value="/details/{id:\\d+}")
    public ResponseEntity<?> deleteBoard(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable Integer id
    ) throws IOException {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 삭제
        boardService.deleteBoard(id, me.getId());

        // 성공 json 반환
        return ResponseEntity.ok(new ApiSuccess(200, "성공적으로 처리되었습니다."));
    }

    // 판매 게시글 등록 전 포인트 차감
    @GetMapping(value="/point")
    public ResponseEntity<?> getPoint(
            @AuthenticationPrincipal CustomUserDetails me
    ) throws IOException {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 보유 포인트 반환
        return ResponseEntity.ok(boardService.getPoint(me.getId()));
    }
}

