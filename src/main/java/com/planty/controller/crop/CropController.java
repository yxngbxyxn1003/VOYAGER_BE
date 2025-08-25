package com.planty.controller.crop;

import com.planty.config.CustomUserDetails;
import com.planty.dto.crop.HomeCropDto;
import com.planty.service.user.UserService;
import com.planty.dto.crop.CropRegistrationDto;
import com.planty.dto.crop.CropDiagnosisRequestDto;
import com.planty.dto.crop.CropDetailAnalysisResult;


import com.planty.entity.crop.AnalysisStatus;
import com.planty.entity.crop.AnalysisType;
import com.planty.entity.crop.Crop;
import com.planty.entity.user.User;
import com.planty.service.crop.CropService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.planty.dto.diary.DiaryFormDto;
import com.planty.entity.diary.Diary;
import com.planty.service.diary.DiaryService;

@Slf4j
@RestController
@RequestMapping("/api/crop")
@RequiredArgsConstructor
public class CropController {

    private final CropService cropService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final DiaryService diaryService;

    /**
     * 작물 목록 페이지
     */
    @GetMapping("")
    @ResponseBody
    public ResponseEntity<List<HomeCropDto>> getHomeCrop(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("[CROP] getHomeCrop called with userDetails: {}", userDetails);
        if (userDetails == null) {
            log.error("[CROP] userDetails is null!");
            return ResponseEntity.status(401).body(null);
        }
        log.info("[CROP] User ID: {}, Username: {}", userDetails.getId(), userDetails.getUsername());
        return ResponseEntity.ok(cropService.getHomeCrop(userDetails.getId()));
    }

    /**
     * 작물 상세정보 조회 (홈에서 작물 클릭 시)
     */
    @GetMapping("/{cropId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCropDetail(
            @PathVariable Integer cropId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> response = new LinkedHashMap<>();

        try {
            User user = userService.findById(userDetails.getId());
            Crop crop = cropService.getCropById(cropId);

            // 권한 확인
            if (!crop.getUser().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("message", "권한이 없습니다.");
                return ResponseEntity.status(403).body(response);
            }

            // 작물 상세정보
            Map<String, Object> cropInfo = new LinkedHashMap<>();
            cropInfo.put("id", crop.getId());
            cropInfo.put("name", crop.getName());
            cropInfo.put("cropImg", crop.getCropImg());
            cropInfo.put("startAt", crop.getStartAt());
            cropInfo.put("endAt", crop.getEndAt());
            cropInfo.put("howTo", crop.getHowTo());
            cropInfo.put("analysisStatus", crop.getAnalysisStatus().toString());
            cropInfo.put("isRegistered", crop.getIsRegistered());
            cropInfo.put("harvest", crop.getHarvest());

            // 같은 종류 작물의 재배일지 목록 조회
            List<Map<String, Object>> cropDiaries = cropService.getCropDiaries(cropId, user.getId());

            response.put("success", true);
            response.put("crop", cropInfo);
            response.put("diaries", cropDiaries);
            response.put("message", "작물 상세정보 조회 성공");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("작물 상세정보 조회 실패", e);
            response.put("success", false);
            response.put("message", "작물 상세정보 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 새로운 통합 등록 방식: 텍스트 데이터와 이미지를 한 번에 받아서 재배방법 분석 후 결과 반환
     */
    @PostMapping(value = "/register")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registerCropWithImage(
            @RequestPart("cropData") String cropDataJson,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            User user = userService.findById(userDetails.getId());
            
            // 받은 JSON 데이터 로깅
            log.info("받은 cropData JSON: {}", cropDataJson);
            log.info("JSON 길이: {} characters", cropDataJson.length());
            
            // JSON 문자열을 CropRegistrationDto로 변환
            CropRegistrationDto cropData;
            try {
                cropData = objectMapper.readValue(cropDataJson, CropRegistrationDto.class);
                log.info("변환된 cropData: {}", cropData);
            } catch (Exception e) {
                log.error("JSON 파싱 오류: {}", e.getMessage());
                log.error("JSON 파싱 실패한 원본 데이터: {}", cropDataJson);
                e.printStackTrace(); // 스택 트레이스 출력
                Map<String, Object> errorResponse = new LinkedHashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "작물 데이터 형식이 올바르지 않습니다. 오류: " + e.getMessage());
                errorResponse.put("receivedData", cropDataJson);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // 텍스트 데이터와 이미지를 한 번에 처리하여 재배방법 분석 결과 반환
            Map<String, Object> analysisResult = cropService.analyzeCropWithData(user, cropData, imageFile);

            // 사용자 입력값만 포함하는 응답 생성
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "재배방법 분석이 완료되었습니다. 최종 등록을 진행해주세요.");
            response.put("analysisType", "재배방법 분석");
            response.put("cropData", Map.of(
                "name", cropData.getName(),
                "startAt", cropData.getStartAt(),
                "endAt", cropData.getEndAt()
            ));
            response.put("analysisResult", analysisResult);
            response.put("tempCropId", analysisResult.get("tempCropId"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("작물 등록 및 재배방법 분석 실패", e);
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "message", "작물 등록에 실패했습니다: " + e.getMessage()
                ));
        }
    }

    /**
     * 최종 등록: 분석 결과와 텍스트 데이터를 DB에 저장
     */
    @PostMapping("/final-register")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> finalizeCropRegistration(
            @RequestBody Map<String, Object> finalData,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> response = new LinkedHashMap<>();

        try {
            User user = userService.findById(userDetails.getId());
            
            // 최종 등록 처리
            Crop savedCrop = cropService.finalizeCropRegistration(user, finalData);

            response.put("success", true);
            response.put("message", "작물이 성공적으로 등록되었습니다.");
            response.put("cropId", savedCrop.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("최종 등록 실패", e);
            response.put("success", false);
            response.put("message", "최종 등록에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 작물 분석 상태 확인 (AJAX)
     */
    @GetMapping("/analysis-status/{cropId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAnalysisStatus(
            @PathVariable Integer cropId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> response = new LinkedHashMap<>();

        try {
            Crop crop = cropService.getCropById(cropId);
            User user = userService.findById(userDetails.getId());

            // 권한 확인
            if (!crop.getUser().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("message", "권한이 없습니다.");
                return ResponseEntity.status(403).body(response);
            }

            response.put("success", true);
            response.put("analysisStatus", crop.getAnalysisStatus().toString());
            response.put("isRegistered", crop.getIsRegistered());

            // 분석 완료된 경우 분석 결과 포함
            if (crop.getAnalysisStatus() == AnalysisStatus.COMPLETED) {
                Map<String, String> analysisResult = new LinkedHashMap<>();
                analysisResult.put("environment", crop.getEnvironment());
                analysisResult.put("temperature", crop.getTemperature());
                analysisResult.put("height", crop.getHeight());
                analysisResult.put("howTo", crop.getHowTo());
                response.put("analysisResult", analysisResult);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("분석 상태 조회 실패", e);
            response.put("success", false);
            response.put("message", "분석 상태 조회에 실패했습니다.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 작물 상세페이지에서 진단받기 위한 태그 선택 정보 조회
     */
    @GetMapping("/{cropId}/diagnosis/tags")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDiagnosisTags(
            @PathVariable Integer cropId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            User user = userService.findById(userDetails.getId());
            
            // 작물 존재 여부 및 권한 확인
            Crop crop = cropService.getCropById(cropId);
            if (!crop.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403)
                    .body(Map.of(
                        "success", false,
                        "message", "권한이 없습니다."
                    ));
            }

            // 진단 가능한 태그들 (CURRENT_STATUS, DISEASE_CHECK, QUALITY_MARKET)
            Map<String, String> diagnosisTags = new LinkedHashMap<>();
            diagnosisTags.put(AnalysisType.CURRENT_STATUS.name(), "현재상태분석");
            diagnosisTags.put(AnalysisType.DISEASE_CHECK.name(), "질병여부");
            diagnosisTags.put(AnalysisType.QUALITY_MARKET.name(), "시장성");

            response.put("success", true);
            response.put("cropId", cropId);
            response.put("cropName", crop.getName());
            response.put("analysisType", "DIAGNOSIS_ANALYSIS");
            response.put("diagnosisTags", diagnosisTags);
            response.put("message", "진단할 태그를 선택해주세요.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("진단 태그 정보 조회 실패 - Crop ID: {}", cropId, e);
            response.put("success", false);
            response.put("message", "진단 태그 정보 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 작물 상세페이지에서 진단받기 (해당 cropID로 진단 진행)
     */
    @PostMapping(value = "/{cropId}/diagnosis")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> analyzeCropDiagnosis(
            @PathVariable Integer cropId,
            @RequestParam("image") MultipartFile image,
            @RequestParam("analysisType") String analysisType,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> response = new LinkedHashMap<>();

        try {
            // 이미지 검증 제거 - 어떤 이미지든 허용

            User user = userService.findById(userDetails.getId());
            
            // 작물 존재 여부 및 권한 확인
            Crop crop = cropService.getCropById(cropId);
            if (!crop.getUser().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("message", "권한이 없습니다.");
                return ResponseEntity.status(403).body(response);
            }

            // AnalysisType enum으로 변환
            AnalysisType analysisTypeEnum;
            try {
                analysisTypeEnum = AnalysisType.valueOf(analysisType);
            } catch (IllegalArgumentException e) {
                response.put("success", false);
                response.put("message", "잘못된 진단 타입입니다: " + analysisType);
                return ResponseEntity.badRequest().body(response);
            }

            // 진단 분석 타입인지 확인 (CURRENT_STATUS, DISEASE_CHECK, QUALITY_MARKET만 허용)
            if (!analysisTypeEnum.isDiagnosisAnalysis()) {
                response.put("success", false);
                response.put("message", "잘못된 진단 타입입니다. 진단 분석만 가능합니다.");
                return ResponseEntity.badRequest().body(response);
            }

            // 해당 cropID로 진단 수행
            CropDetailAnalysisResult result = cropService.analyzeCropDiagnosis(cropId, user, analysisTypeEnum, image);

            if (result.isSuccess()) {
                response.put("success", true);
                response.put("message", "작물 진단이 완료되었습니다.");
                response.put("cropId", cropId);
                response.put("analysisType", analysisType);
                response.put("diagnosisResult", result.getAnalysisResult());
            } else {
                response.put("success", false);
                response.put("message", result.getAnalysisMessage());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("작물 진단 실패 - Crop ID: {}, 오류: {}", cropId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "진단에 실패했습니다. 잠시 후 다시 시도해주세요.");
            return ResponseEntity.status(500).body(response);
        }
    }


    /**
     * 진단결과 기반 재배일지 생성 (진단 완료 후 재배일지 작성)
     */
    @PostMapping("/diagnosis-diary/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createDiagnosisBasedDiary(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> response = new LinkedHashMap<>();

        try {
            User user = userService.findById(userDetails.getId());
            String title = (String) request.get("title");
            String content = (String) request.get("content");
            String diagnosisType = (String) request.get("diagnosisType");
            @SuppressWarnings("unchecked")
            Map<String, Object> diagnosisResult = (Map<String, Object>) request.get("diagnosisResult");
            Boolean includeDiagnosis = (Boolean) request.get("includeDiagnosis");
            @SuppressWarnings("unchecked")
            List<String> imageUrls = (List<String>) request.get("imageUrls");

            // cropId 추출 (진단 결과에서)
            Integer cropId = (Integer) request.get("cropId");
            if (cropId == null) {
                response.put("success", false);
                response.put("message", "작물 ID가 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }

            // DiaryService를 사용하여 진단결과 기반 재배일지 생성
            Diary createdDiary = diaryService.createDiagnosisBasedDiary(
                user.getId(), cropId, title, content, diagnosisType, 
                diagnosisResult, includeDiagnosis, imageUrls
            );

            response.put("success", true);
            response.put("message", "진단결과 기반 재배일지가 생성되었습니다.");
            response.put("diaryId", createdDiary.getId());
            response.put("diaryTitle", createdDiary.getTitle());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("진단결과 기반 재배일지 생성 실패", e);
            response.put("success", false);
            response.put("message", "재배일지 생성에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 작물 재배 완료 상태 변경
     */
    @PutMapping("/{cropId}/harvest-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateHarvestStatus(
            @PathVariable Integer cropId,
            @RequestBody Map<String, Boolean> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> response = new LinkedHashMap<>();

        try {
            User user = userService.findById(userDetails.getId());
            Boolean harvestStatus = request.get("harvest");
            
            if (harvestStatus == null) {
                response.put("success", false);
                response.put("message", "재배 완료 상태가 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }

            Crop updatedCrop = cropService.updateHarvestStatus(cropId, user, harvestStatus);

            response.put("success", true);
            response.put("message", harvestStatus ? "재배 완료로 설정되었습니다." : "재배 중으로 설정되었습니다.");
            response.put("cropId", updatedCrop.getId());
            response.put("harvest", updatedCrop.getHarvest());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("재배 완료 상태 변경 실패", e);
            response.put("success", false);
            response.put("message", "상태 변경에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 작물 정보 수정 (이름, 날짜, 이미지 포함)
     */
    @PutMapping(value = "/{cropId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateCrop(
            @PathVariable Integer cropId,
            @RequestPart(value = "cropData", required = false) String cropDataJson,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> response = new LinkedHashMap<>();

        try {
            User user = userService.findById(userDetails.getId());
            
            // JSON 데이터 파싱
            CropRegistrationDto updateDto = null;
            if (cropDataJson != null && !cropDataJson.isBlank()) {
                updateDto = objectMapper.readValue(cropDataJson, CropRegistrationDto.class);
            }
            
            // 이미지 파일이 있는 경우 이미지 업데이트 (검증 없음)
            if (imageFile != null && !imageFile.isEmpty()) {
                // 이미지 검증 제거 - 어떤 이미지든 허용
            }
            
            // 작물 정보 업데이트 (이미지 포함)
            Crop updatedCrop = cropService.updateCropWithImage(cropId, user, updateDto, imageFile);

            response.put("success", true);
            response.put("message", "작물 정보가 성공적으로 수정되었습니다.");
            response.put("cropId", updatedCrop.getId());
            response.put("cropName", updatedCrop.getName());
            response.put("startAt", updatedCrop.getStartAt());
            response.put("endAt", updatedCrop.getEndAt());
            response.put("cropImg", updatedCrop.getCropImg());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("작물 정보 수정 실패", e);
            response.put("success", false);
            response.put("message", "작물 정보 수정에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 작물 삭제
     */
    @DeleteMapping("/{cropId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteCrop(
            @PathVariable Integer cropId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> response = new LinkedHashMap<>();

        try {
            User user = userService.findById(userDetails.getId());
            
            cropService.deleteCrop(cropId, user);

            response.put("success", true);
            response.put("message", "작물이 성공적으로 삭제되었습니다.");
            response.put("cropId", cropId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("작물 삭제 실패", e);
            response.put("success", false);
            response.put("message", "작물 삭제에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/home-harvest")
    @ResponseBody
    public ResponseEntity<List<HomeCropDto>> getHome(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(cropService.getHomeCrop(userDetails.getId()));
    }
}