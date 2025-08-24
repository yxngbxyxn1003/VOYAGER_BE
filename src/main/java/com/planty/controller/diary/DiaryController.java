package com.planty.controller.diary;

import com.planty.common.ApiSuccess;
import com.planty.config.CustomUserDetails;

import com.planty.dto.diary.*;
import com.planty.dto.crop.HomeCropDto;
import com.planty.entity.crop.Crop;


import com.planty.service.diary.DiaryService;
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


// 재배일지 컨트롤러
@RestController
@RequestMapping("/api/diary")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;
    private final StorageService storageService;

    // 재배일지 작성용 사용자 작물 목록 조회
    // 이제 CropController의 /api/crop/home-crops를 사용합니다
    /*
    @GetMapping("/crops")
    public ResponseEntity<List<HomeCropDto>> getUserCrops(
            @AuthenticationPrincipal CustomUserDetails me
    ) {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 사용자 작물 목록 반환
        return ResponseEntity.ok(diaryService.getUserCrops(me.getId()));
    }
    */

    // 재배일지 등록 (JSON+파일)
    @PostMapping(value="/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createDiary(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestPart("form") @Validated DiaryFormDto form,  // 재배일지 데이터
            @RequestPart(value = "images", required = false) List<MultipartFile> images // 재배일지 이미지
    ) throws IOException {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 이미지 개수 검증 (최대 9개)
        if (images != null && images.size() > 9) {
            return ResponseEntity.badRequest()
                    .body(new ApiSuccess(400, "이미지는 최대 9개까지만 업로드할 수 있습니다."));
        }

        // 파일 저장 → URL 리스트 생성
        List<String> urls = new ArrayList<>();
        if (images != null) {
            for (MultipartFile f : images) {
                if (!f.isEmpty()) {
                    urls.add(storageService.save(f, "diary"));
                }
            }
        }

        // 이미지 URL 개수 재검증 (빈 파일 제외 후)
        if (urls.size() > 9) {
            return ResponseEntity.badRequest()
                    .body(new ApiSuccess(400, "유효한 이미지는 최대 9개까지만 업로드할 수 있습니다."));
        }

        // 서비스 호출
        diaryService.saveDiary(me.getId(), form, urls);

        // 성공 응답
        return ResponseEntity.status(201).body(new ApiSuccess(201, "재배일지가 성공적으로 작성되었습니다."));
    }

    // 재배일지 상세 조회
    @GetMapping("/details/{id}")
    public ResponseEntity<DiaryDetailResDto> getDiaryDetail(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable Integer id
    ) {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 재배일지 데이터 가져오기
        DiaryDetailResDto dto = diaryService.getDiaryDetail(id, me.getId());

        // 재배일지 데이터 반환
        return ResponseEntity.ok(dto);
    }

    // 내 재배일지 목록 조회 (같은 분류 작물만)
    @GetMapping("/my")
    public ResponseEntity<List<DiaryListDto>> getMyDiaries(
            @AuthenticationPrincipal CustomUserDetails me
    ) {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 내 재배일지 목록 가져오기 (같은 분류 작물만)
        List<DiaryListDto> diaries = diaryService.getMyDiariesByCategory(me.getId());

        return ResponseEntity.ok(diaries);
    }

    // 작물별 재배일지 목록 조회
    @GetMapping("/crop/{cropId}")
    public ResponseEntity<List<DiaryListDto>> getCropDiaries(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable Integer cropId
    ) {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 작물별 재배일지 목록 가져오기
        List<DiaryListDto> diaries = diaryService.getCropDiaries(cropId);

        return ResponseEntity.ok(diaries);
    }

    // 재배일지 수정
    @PutMapping(value="/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateDiary(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable Integer id,
            @RequestPart("form") @Validated DiaryUpdateDto form,  // 수정할 재배일지 데이터
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages // 새로 추가할 이미지
    ) throws IOException {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 새 이미지 개수 검증 (기존 이미지 + 새 이미지 ≤ 9개)
        if (newImages != null && newImages.size() > 9) {
            return ResponseEntity.badRequest()
                    .body(new ApiSuccess(400, "새로 추가할 이미지는 최대 9개까지만 업로드할 수 있습니다."));
        }

        // 파일 저장 → URL 리스트 생성
        List<String> newImageUrls = new ArrayList<>();
        if (newImages != null) {
            for (MultipartFile f : newImages) {
                if (!f.isEmpty()) {
                    newImageUrls.add(storageService.save(f, "diary"));
                }
            }
        }

        // 서비스 호출
        diaryService.updateDiary(id, me.getId(), form, newImageUrls);

        // 성공 응답
        return ResponseEntity.ok(new ApiSuccess(200, "재배일지가 성공적으로 수정되었습니다."));
    }

    // 재배일지 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDiary(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable Integer id
    ) {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 재배일지 삭제 (권한 체크 포함)
        diaryService.deleteDiary(id, me.getId());

        // 성공 응답
        return ResponseEntity.ok(new ApiSuccess(200, "재배일지가 성공적으로 삭제되었습니다."));
    }
}
