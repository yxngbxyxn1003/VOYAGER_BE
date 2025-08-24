package com.planty.controller.crop;

import com.planty.config.CustomUserDetails;
import com.planty.service.user.UserService;
import com.planty.dto.crop.CropRegistrationDto;
import com.planty.dto.crop.CropDiagnosisRequestDto;
import com.planty.dto.crop.CropDetailAnalysisResult;

import com.planty.entity.crop.AnalysisStatus;
import com.planty.entity.crop.Crop;
import com.planty.entity.user.User;
import com.planty.service.crop.CropService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.HashMap;

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
    @GetMapping
    public String cropList(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userService.findById(userDetails.getId());
        List<Crop> crops = cropService.getUserCrops(user);

        model.addAttribute("crops", crops);
        return "crop/crop-list";
    }

    /**
     * 홈 화면용 작물 목록 조회 (등록된 것과 미등록된 것 모두)
     */
    @GetMapping("/home-crops")
    @ResponseBody
    public ResponseEntity<List<com.planty.dto.crop.HomeCropDto>> getHomeCrops(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            User user = userService.findById(userDetails.getId());
            List<com.planty.dto.crop.HomeCropDto> homeCrops = cropService.getHomeCrops(user);
            
            return ResponseEntity.ok(homeCrops);
            
        } catch (Exception e) {
            log.error("홈 화면 작물 목록 조회 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 간단한 이미지 업로드 테스트용 엔드포인트
     */
    @PostMapping(value = "/test-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testImageUpload(
            @RequestParam("imageFile") MultipartFile imageFile) {

        Map<String, Object> response = new LinkedHashMap<>();

        try {
            // 파일 검증
            if (imageFile == null || imageFile.isEmpty()) {
                response.put("success", false);
                response.put("message", "이미지 파일이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }

            // 파일 정보 로깅
            log.info("업로드된 파일 정보:");
            log.info("원본 파일명: {}", imageFile.getOriginalFilename());
            log.info("파일 크기: {} bytes", imageFile.getSize());
            log.info("Content-Type: {}", imageFile.getContentType());

            // 로컬 개발용 업로드 경로
            String uploadPath = System.getProperty("user.dir") + "/uploads/test";
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            // 배포용 경로 (주석처리)
            /*
            // 실제 업로드 경로로 저장 테스트
            String uploadPath = "/home/ec2-user/planty/uploads/test";
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            */
            
            String fileName = "test_" + System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
            File uploadFile = new File(uploadDir, fileName);
            
            imageFile.transferTo(uploadFile);
            
            response.put("success", true);
            response.put("message", "이미지 업로드 테스트 성공");
            response.put("filePath", uploadFile.getAbsolutePath());
            response.put("fileSize", imageFile.getSize());
            response.put("originalFilename", imageFile.getOriginalFilename());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("이미지 업로드 테스트 실패", e);
            response.put("success", false);
            response.put("message", "이미지 업로드 테스트 실패: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            return ResponseEntity.badRequest().body(response);
        }
    }



    /**
     * 통합 작물등록: 이름, 날짜, 이미지만 처리
     */
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> registerCrop(
            @RequestPart("cropData") String cropDataJson,
            @RequestPart("imageFile") MultipartFile imageFile,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> response = new HashMap<>();

        try {
            User user = userService.findById(userDetails.getId());
            
            // JSON 데이터 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            CropRegistrationDto dto = objectMapper.readValue(cropDataJson, CropRegistrationDto.class);
            
            // 이미지 파일 설정
            dto.setImageFile(imageFile);
            
            // 통합 작물등록 처리
            Crop crop = cropService.registerCropWithImage(user, dto);

            response.put("success", true);
            response.put("cropId", crop.getId());
            response.put("message", "작물이 성공적으로 등록되었습니다. AI 분석을 시작합니다.");
            response.put("analysisStatus", crop.getAnalysisStatus().toString());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("통합 작물등록 실패", e);
            response.put("success", false);
            response.put("message", "작물 등록에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
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
            Integer cropId = (Integer) finalData.get("cropId");
            if (cropId == null) {
                response.put("success", false);
                response.put("message", "작물 ID가 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            Crop savedCrop = cropService.completeCropRegistration(cropId, user);

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
     * 작물 태그별 진단 실행 (태그 선택 후)
     */
    @PostMapping("/analyze-diagnosis")
    @ResponseBody
    public ResponseEntity<CropDetailAnalysisResult> analyzeCropDiagnosis(
            @RequestBody CropDiagnosisRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            User user = userService.findById(userDetails.getId());
            Crop crop = cropService.getCropById(request.getCropId());

            // 권한 확인
            if (!crop.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403)
                    .body(new CropDetailAnalysisResult(false, "권한이 없습니다.", request.getAnalysisType()));
            }

            // 등록된 작물만 진단 가능
            if (!crop.getIsRegistered()) {
                return ResponseEntity.badRequest()
                    .body(new CropDetailAnalysisResult(false, "등록되지 않은 작물은 진단할 수 없습니다.", request.getAnalysisType()));
            }

            // 진단 분석 타입인지 확인
            if (!request.getAnalysisType().isDiagnosisAnalysis()) {
                return ResponseEntity.badRequest()
                    .body(new CropDetailAnalysisResult(false, "잘못된 진단 타입입니다.", request.getAnalysisType()));
            }

            // 태그별 진단 수행
            CropDetailAnalysisResult result = cropService.analyzeCropDetail(crop, request.getAnalysisType());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("작물 태그별 진단 실패", e);
            return ResponseEntity.badRequest()
                .body(new CropDetailAnalysisResult(false, "진단에 실패했습니다: " + e.getMessage(), request.getAnalysisType()));
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
                // LocalDate 지원을 위한 모듈 등록 (필수!)
                objectMapper.findAndRegisterModules();
                objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                updateDto = objectMapper.readValue(cropDataJson, CropRegistrationDto.class);
            }
            
            // 이미지 파일이 있는 경우 이미지 업데이트 (null만 아니면 모든 이미지 허용)
            if (imageFile != null) {
                // 이미지 파일 검증 없이 모든 파일 허용
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
