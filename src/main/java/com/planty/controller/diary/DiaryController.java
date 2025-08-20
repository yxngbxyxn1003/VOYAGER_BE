package com.planty.controller.diary;

import com.planty.common.ApiSuccess;
import com.planty.config.CustomUserDetails;

import com.planty.dto.diary.*;
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
//
    // 재배일지 작성용 사용자 작물 목록 조회
    @GetMapping("/crops")
    public ResponseEntity<List<Crop>> getUserCrops(
            @AuthenticationPrincipal CustomUserDetails me
    ) {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 사용자 작물 목록 반환
        return ResponseEntity.ok(diaryService.getUserCrops(me.getId()));
    }
//
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
//
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
//
//    // 사용자별 재배일지 목록 조회
//    @GetMapping("/user/{userId}")
//    public ResponseEntity<List<DiaryListDto>> getUserDiaries(
//            @AuthenticationPrincipal CustomUserDetails me,
//            @PathVariable Integer userId
//    ) {
//        // 권한이 없을 때
//        if (me == null) return ResponseEntity.status(401).build();
//
//        // 사용자별 재배일지 목록 가져오기
//        List<DiaryListDto> diaries = diaryService.getUserDiaries(userId);
//
//        return ResponseEntity.ok(diaries);
//    }
//
    // 내 재배일지 목록 조회
    @GetMapping("/my")
    public ResponseEntity<List<DiaryListDto>> getMyDiaries(
            @AuthenticationPrincipal CustomUserDetails me
    ) {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 내 재배일지 목록 가져오기
        List<DiaryListDto> diaries = diaryService.getUserDiaries(me.getId());

        return ResponseEntity.ok(diaries);
    }
//
//    // 작물별 재배일지 목록 조회
//    @GetMapping("/crop/{cropId}")
//    public ResponseEntity<List<DiaryListDto>> getCropDiaries(
//            @AuthenticationPrincipal CustomUserDetails me,
//            @PathVariable Integer cropId
//    ) {
//        // 권한이 없을 때
//        if (me == null) return ResponseEntity.status(401).build();
//
//        // 작물별 재배일지 목록 가져오기
//        List<DiaryListDto> diaries = diaryService.getCropDiaries(cropId);
//
//        return ResponseEntity.ok(diaries);
//    }
//
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
//
//    // 특정 작물의 분석 결과를 포함한 재배일지 작성 폼 데이터 조회 (AI 분석 완료된 작물)
//    @GetMapping("/form-data/{cropId}")
//    public ResponseEntity<?> getDiaryFormDataWithCropAnalysis(
//            @AuthenticationPrincipal CustomUserDetails me,
//            @PathVariable Integer cropId
//    ) {
//        // 권한이 없을 때
//        if (me == null) return ResponseEntity.status(401).build();
//
//        try {
//            Crop crop = cropService.getCropById(cropId);
//
//            // 권한 확인
//            if (!crop.getUser().getId().equals(me.getId())) {
//                return ResponseEntity.status(403)
//                    .body(new ApiSuccess(403, "권한이 없습니다."));
//            }
//
//            // 분석이 완료된 작물만 가능
//            if (crop.getAnalysisStatus() != AnalysisStatus.COMPLETED) {
//                return ResponseEntity.badRequest()
//                    .body(new ApiSuccess(400, "분석이 완료되지 않은 작물입니다."));
//            }
//
//            // 응답 데이터 구성
//            Map<String, Object> response = new HashMap<>();
//
//            // 작물 기본 정보
//            Map<String, Object> cropInfo = new HashMap<>();
//            cropInfo.put("id", crop.getId());
//            cropInfo.put("name", crop.getName());
//            cropInfo.put("cropImg", crop.getCropImg());
//            cropInfo.put("startAt", crop.getStartAt());
//            cropInfo.put("endAt", crop.getEndAt());
//
//            // AI 분석 결과
//            Map<String, String> analysisResult = new HashMap<>();
//            analysisResult.put("environment", crop.getEnvironment());
//            analysisResult.put("temperature", crop.getTemperature());
//            analysisResult.put("height", crop.getHeight());
//            analysisResult.put("howTo", crop.getHowTo());
//
//            // 분석 결과를 텍스트로 변환 (미리보기용)
//            StringBuilder analysisPreview = new StringBuilder();
//            if (crop.getEnvironment() != null && !crop.getEnvironment().trim().isEmpty()) {
//                analysisPreview.append("환경: ").append(crop.getEnvironment()).append("\n\n");
//            }
//            if (crop.getTemperature() != null && !crop.getTemperature().trim().isEmpty()) {
//                analysisPreview.append("온도: ").append(crop.getTemperature()).append("\n\n");
//            }
//            if (crop.getHeight() != null && !crop.getHeight().trim().isEmpty()) {
//                analysisPreview.append("높이: ").append(crop.getHeight()).append("\n\n");
//            }
//            if (crop.getHowTo() != null && !crop.getHowTo().trim().isEmpty()) {
//                analysisPreview.append("재배법: ").append(crop.getHowTo()).append("\n\n");
//            }
//
//            response.put("crop", cropInfo);
//            response.put("analysisResult", analysisResult);
//            response.put("analysisPreview", analysisPreview.toString().trim());
//            response.put("hasAnalysis", true);
//            response.put("success", true);
//            response.put("message", "재배일지 작성 폼 데이터를 성공적으로 가져왔습니다.");
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            return ResponseEntity.badRequest()
//                .body(new ApiSuccess(400, "데이터 조회에 실패했습니다: " + e.getMessage()));
//        }
//    }
//
//    // 일반 재배일지 작성을 위한 작물 정보 조회 (분석 상태 무관)
//    @GetMapping("/form-data/general/{cropId}")
//    public ResponseEntity<?> getDiaryFormDataGeneral(
//            @AuthenticationPrincipal CustomUserDetails me,
//            @PathVariable Integer cropId
//    ) {
//        // 권한이 없을 때
//        if (me == null) return ResponseEntity.status(401).build();
//
//        try {
//            Crop crop = cropService.getCropById(cropId);
//
//            // 권한 확인
//            if (!crop.getUser().getId().equals(me.getId())) {
//                return ResponseEntity.status(403)
//                    .body(new ApiSuccess(403, "권한이 없습니다."));
//            }
//
//            // 등록된 작물만 가능
//            if (!crop.getIsRegistered()) {
//                return ResponseEntity.badRequest()
//                    .body(new ApiSuccess(400, "등록되지 않은 작물입니다."));
//            }
//
//            // 응답 데이터 구성
//            Map<String, Object> response = new HashMap<>();
//
//            // 작물 기본 정보
//            Map<String, Object> cropInfo = new HashMap<>();
//            cropInfo.put("id", crop.getId());
//            cropInfo.put("name", crop.getName());
//            cropInfo.put("cropImg", crop.getCropImg());
//            cropInfo.put("startAt", crop.getStartAt());
//            cropInfo.put("endAt", crop.getEndAt());
//
//            // AI 분석 결과 (있는 경우만)
//            Map<String, String> analysisResult = null;
//            String analysisPreview = null;
//            boolean hasAnalysis = crop.getAnalysisStatus() == AnalysisStatus.COMPLETED;
//
//            if (hasAnalysis) {
//                analysisResult = new HashMap<>();
//                analysisResult.put("environment", crop.getEnvironment());
//                analysisResult.put("temperature", crop.getTemperature());
//                analysisResult.put("height", crop.getHeight());
//                analysisResult.put("howTo", crop.getHowTo());
//
//                // 분석 결과를 텍스트로 변환 (미리보기용)
//                StringBuilder analysisBuilder = new StringBuilder();
//                if (crop.getEnvironment() != null && !crop.getEnvironment().trim().isEmpty()) {
//                    analysisBuilder.append("환경: ").append(crop.getEnvironment()).append("\n\n");
//                }
//                if (crop.getTemperature() != null && !crop.getTemperature().trim().isEmpty()) {
//                    analysisBuilder.append("온도: ").append(crop.getTemperature()).append("\n\n");
//                }
//                if (crop.getHeight() != null && !crop.getHeight().trim().isEmpty()) {
//                    analysisBuilder.append("높이: ").append(crop.getHeight()).append("\n\n");
//                }
//                if (crop.getHowTo() != null && !crop.getHowTo().trim().isEmpty()) {
//                    analysisBuilder.append("재배법: ").append(crop.getHowTo()).append("\n\n");
//                }
//                analysisPreview = analysisBuilder.toString().trim();
//            }
//
//            response.put("crop", cropInfo);
//            response.put("hasAnalysis", hasAnalysis);
//            if (hasAnalysis) {
//                response.put("analysisResult", analysisResult);
//                response.put("analysisPreview", analysisPreview);
//            }
//            response.put("success", true);
//            response.put("message", "재배일지 작성 폼 데이터를 성공적으로 가져왔습니다.");
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            return ResponseEntity.badRequest()
//                .body(new ApiSuccess(400, "데이터 조회에 실패했습니다: " + e.getMessage()));
//        }
//    }
}
