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

@Slf4j
@Controller
@RequestMapping("/api/crop")
@RequiredArgsConstructor
public class CropController {

    private final CropService cropService;
    private final UserService userService;

    /**
     * 작물 목록 페이지
     */
    @GetMapping("")
    public ResponseEntity<List<HomeCropDto>> getHomeCrop(@AuthenticationPrincipal CustomUserDetails userDetails) {
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
            cropInfo.put("environment", crop.getEnvironment());
            cropInfo.put("temperature", crop.getTemperature());
            cropInfo.put("height", crop.getHeight());
            cropInfo.put("howTo", crop.getHowTo());
            cropInfo.put("analysisStatus", crop.getAnalysisStatus().toString());
            cropInfo.put("isRegistered", crop.getIsRegistered());
            cropInfo.put("harvest", crop.getHarvest());
            cropInfo.put("createdAt", crop.getCreatedAt());
            cropInfo.put("modifiedAt", crop.getModifiedAt());

            // 같은 종류 작물의 재배일지 목록 조회
            List<Map<String, Object>> cropDiaries = cropService.getCropDiariesByCategory(cropId, user.getId());

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
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
            ObjectMapper objectMapper = new ObjectMapper();
            // 날짜 형식 설정
            objectMapper.findAndRegisterModules();
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
            response.put("analysisType", "REGISTRATION_ANALYSIS");
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
     * 작물 태그별 진단 페이지 정보 조회 (진단할 작물 정보 + 태그 선택 UI)
     */
    @GetMapping("/{cropId}/diagnosis")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCropDiagnosisPage(
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

            // 등록된 작물만 진단 가능
            if (!crop.getIsRegistered()) {
                response.put("success", false);
                response.put("message", "등록되지 않은 작물은 진단할 수 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }

            // 작물 정보
            Map<String, Object> cropInfo = new LinkedHashMap<>();
            cropInfo.put("id", crop.getId());
            cropInfo.put("name", crop.getName());
            cropInfo.put("cropImg", crop.getCropImg());
            cropInfo.put("startAt", crop.getStartAt());
            cropInfo.put("endAt", crop.getEndAt());

            // 태그별 진단 옵션들 (DIAGNOSIS_ANALYSIS 타입들)
            Map<String, String> diagnosisOptions = new LinkedHashMap<>();
            diagnosisOptions.put("CURRENT_STATUS", "현재상태분석");
            diagnosisOptions.put("DISEASE_CHECK", "질병여부");
            diagnosisOptions.put("QUALITY_MARKET", "시장성");

            response.put("success", true);
            response.put("analysisType", "DIAGNOSIS_ANALYSIS");  // AI 분석 타입 명시
            response.put("crop", cropInfo);
            response.put("diagnosisOptions", diagnosisOptions);
            response.put("message", "진단할 태그를 선택해주세요.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("작물 태그별 진단 페이지 조회 실패", e);
            response.put("success", false);
            response.put("message", "정보 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 작물 태그별 진단 실행 (새 이미지 업로드)
     */
    @PostMapping(value = "/{cropId}/diagnosis/{analysisType}/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<CropDetailAnalysisResult> analyzeCropDiagnosisWithNewImage(
            @PathVariable Integer cropId,
            @PathVariable String analysisType,
            @RequestParam("newImage") MultipartFile newImage,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            // AnalysisType enum으로 변환
            AnalysisType analysisTypeEnum;
            try {
                analysisTypeEnum = AnalysisType.valueOf(analysisType);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(new CropDetailAnalysisResult(false, "잘못된 진단 타입입니다: " + analysisType, null));
            }
            
            User user = userService.findById(userDetails.getId());
            Crop crop = cropService.getCropById(cropId);

            // 권한 확인
            if (!crop.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403)
                    .body(new CropDetailAnalysisResult(false, "권한이 없습니다.", analysisTypeEnum));
            }

            // 등록된 작물만 진단 가능
            if (!crop.getIsRegistered()) {
                return ResponseEntity.badRequest()
                    .body(new CropDetailAnalysisResult(false, "등록되지 않은 작물은 진단할 수 없습니다.", analysisTypeEnum));
            }

            // 진단 분석 타입인지 확인
            if (!analysisTypeEnum.isDiagnosisAnalysis()) {
                return ResponseEntity.badRequest()
                    .body(new CropDetailAnalysisResult(false, "잘못된 진단 타입입니다.", analysisTypeEnum));
            }

            // 새 이미지로 진단 수행
            CropDetailAnalysisResult result = cropService.analyzeCropDetailWithNewImage(crop, analysisTypeEnum, newImage);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("새 이미지로 작물 진단 실패", e);
            return ResponseEntity.badRequest()
                .body(new CropDetailAnalysisResult(false, "진단에 실패했습니다: " + e.getMessage(), null));
        }
    }

    /**
     * 진단 결과를 바탕으로 재배일지 작성 폼 데이터 조회
     */
    @PostMapping("/diagnosis-to-diary")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createDiaryWithDiagnosis(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> response = new LinkedHashMap<>();

        try {
            User user = userService.findById(userDetails.getId());
            Integer cropId = (Integer) request.get("cropId");
            String diagnosisType = (String) request.get("diagnosisType");
            @SuppressWarnings("unchecked")
            Map<String, Object> diagnosisResult = (Map<String, Object>) request.get("diagnosisResult");

            Crop crop = cropService.getCropById(cropId);

            // 권한 확인
            if (!crop.getUser().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("message", "권한이 없습니다.");
                return ResponseEntity.status(403).body(response);
            }

            // 작물 정보
            Map<String, Object> cropInfo = new LinkedHashMap<>();
            cropInfo.put("id", crop.getId());
            cropInfo.put("name", crop.getName());
            cropInfo.put("cropImg", crop.getCropImg());

            // 진단 결과를 재배일지 템플릿으로 변환
            String diaryTemplate = createDiaryTemplate(diagnosisType, diagnosisResult);

            response.put("success", true);
            response.put("crop", cropInfo);
            response.put("diagnosisType", diagnosisType);
            response.put("diagnosisResult", diagnosisResult);
            response.put("diaryTemplate", diaryTemplate);
            response.put("message", "재배일지 작성 준비가 완료되었습니다.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("진단 결과 기반 재배일지 준비 실패", e);
            response.put("success", false);
            response.put("message", "재배일지 준비에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 진단 결과를 재배일지 템플릿으로 변환
     */
    private String createDiaryTemplate(String diagnosisType, Map<String, Object> diagnosisResult) {
        StringBuilder template = new StringBuilder();
        template.append("# AI 진단 결과 기반 재배일지\n\n");

        switch (diagnosisType) {
            case "CURRENT_STATUS":
                template.append("## 현재 상태 분석\n");
                template.append(diagnosisResult.getOrDefault("currentStatusSummary", "분석 결과 없음"));
                break;
            case "DISEASE_CHECK":
                template.append("## 질병 진단 결과\n");
                template.append("**질병 상태:** ").append(diagnosisResult.getOrDefault("diseaseStatus", "")).append("\n");
                template.append("**상세 내용:** ").append(diagnosisResult.getOrDefault("diseaseDetails", "")).append("\n");
                template.append("**예방 및 치료 방법:** ").append(diagnosisResult.getOrDefault("preventionMethods", "")).append("\n");
                break;
            case "QUALITY_MARKET":
                template.append("## 품질 및 시장성 분석\n");
                template.append("**상품 비율:** ").append(diagnosisResult.getOrDefault("marketRatio", "")).append("\n");
                template.append("**색상 품질:** ").append(diagnosisResult.getOrDefault("colorUniformity", "")).append("\n");
                template.append("**저장성:** ").append(diagnosisResult.getOrDefault("storageEvaluation", "")).append("\n");
                break;
        }

        template.append("\n\n## 오늘의 관찰 사항\n");
        template.append("(여기에 오늘 관찰한 내용을 추가로 작성해주세요)\n\n");

        return template.toString();
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
    @PutMapping(value = "/{cropId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
                ObjectMapper objectMapper = new ObjectMapper();
                updateDto = objectMapper.readValue(cropDataJson, CropRegistrationDto.class);
            }
            
            // 이미지 파일이 있는 경우 이미지 업데이트
            if (imageFile != null && !imageFile.isEmpty()) {
                // 파일 크기 검증 (10MB 제한)
                if (imageFile.getSize() > 10 * 1024 * 1024) {
                    response.put("success", false);
                    response.put("message", "이미지 파일 크기는 10MB 이하여야 합니다.");
                    return ResponseEntity.badRequest().body(response);
                }

                // 파일 형식 검증
                String contentType = imageFile.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    response.put("success", false);
                    response.put("message", "이미지 파일만 업로드 가능합니다.");
                    return ResponseEntity.badRequest().body(response);
                }
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
}
