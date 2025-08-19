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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/crop")
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
     * 1단계: 작물 기본 정보 등록 (이름, 재배시작일, 수확예정일)
     */
    @PostMapping("/create-temp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createTempCrop(
            @RequestBody CropRegistrationDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> response = new HashMap<>();

        try {
            User user = userService.findById(userDetails.getId());
            Crop crop = cropService.createTempCrop(user, dto);

            response.put("success", true);
            response.put("cropId", crop.getId());
            response.put("message", "작물 기본 정보가 저장되었습니다. 이미지를 업로드해주세요.");
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("작물 기본 정보 등록 실패", e);
            response.put("success", false);
            response.put("message", "작물 정보 등록에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 2단계: 임시 등록된 작물에 이미지 업로드 및 AI 분석
     */
    @PostMapping("/{cropId}/upload-image")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadImageToExistingCrop(
            @PathVariable Integer cropId,
            @RequestParam("imageFile") MultipartFile imageFile,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> response = new HashMap<>();

        try {
            User user = userService.findById(userDetails.getId());
            Crop crop = cropService.uploadCropImageToExisting(cropId, imageFile);

            // 권한 확인
            if (!crop.getUser().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("message", "권한이 없습니다.");
                return ResponseEntity.status(403).body(response);
            }

            response.put("success", true);
            response.put("cropId", crop.getId());
            response.put("message", "이미지가 업로드되었습니다. AI 분석을 시작합니다.");
            response.put("analysisStatus", crop.getAnalysisStatus().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("이미지 업로드 실패", e);
            response.put("success", false);
            response.put("message", "이미지 업로드에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 기존 방식: 작물 이미지 업로드 (호환성 유지)
     */
    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadCropImage(
            @RequestParam("imageFile") MultipartFile imageFile,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> response = new HashMap<>();

        try {
            User user = userService.findById(userDetails.getId());
            Crop crop = cropService.uploadCropImage(user, imageFile);

            response.put("success", true);
            response.put("cropId", crop.getId());
            response.put("message", "이미지가 업로드되었습니다. 분석을 시작합니다.");
            response.put("analysisStatus", crop.getAnalysisStatus().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("이미지 업로드 실패", e);
            response.put("success", false);
            response.put("message", "이미지 업로드에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
//
    /**
     * 작물 분석 상태 확인 (AJAX)
     */
    @GetMapping("/analysis-status/{cropId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAnalysisStatus(
            @PathVariable Integer cropId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> response = new HashMap<>();

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
                Map<String, String> analysisResult = new HashMap<>();
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
//
//    /**
//     * 작물 등록 완료 (이름, 날짜 입력 후)
//     */
//    @PostMapping("/complete-registration")
//    @ResponseBody
//    public ResponseEntity<Map<String, Object>> completeCropRegistration(
//            @RequestBody CropRegistrationDto dto,
//            @AuthenticationPrincipal CustomUserDetails userDetails) {
//
//        Map<String, Object> response = new HashMap<>();
//
//        try {
//            User user = userService.findById(userDetails.getId());
//            Crop crop = cropService.getCropById(dto.getId());
//
//            // 권한 확인
//            if (!crop.getUser().getId().equals(user.getId())) {
//                response.put("success", false);
//                response.put("message", "권한이 없습니다.");
//                return ResponseEntity.status(403).body(response);
//            }
//
//            Crop completedCrop = cropService.completeCropRegistration(dto.getId(), dto);
//
//            response.put("success", true);
//            response.put("message", "작물 등록이 완료되었습니다.");
//            response.put("crop", completedCrop);
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("작물 등록 완료 실패", e);
//            response.put("success", false);
//            response.put("message", "작물 등록에 실패했습니다: " + e.getMessage());
//            return ResponseEntity.badRequest().body(response);
//        }
//    }
//
//    /**
//     * 작물 상세 정보 조회
//     */
//    @GetMapping("/{cropId}")
//    public String cropDetail(@PathVariable Integer cropId,
//                           @AuthenticationPrincipal CustomUserDetails userDetails,
//                           Model model) {
//        try {
//            Crop crop = cropService.getCropById(cropId);
//            User user = userService.findById(userDetails.getId());
//
//            // 권한 확인
//            if (!crop.getUser().getId().equals(user.getId())) {
//                model.addAttribute("error", "권한이 없습니다.");
//                return "error/403";
//            }
//
//            model.addAttribute("crop", crop);
//            return "crop/crop-detail";
//
//        } catch (Exception e) {
//            log.error("작물 상세 조회 실패", e);
//            model.addAttribute("error", "작물 정보를 찾을 수 없습니다.");
//            return "error/404";
//        }
//    }
//
//    /**
//     * 홈 화면용 작물 목록 조회 (등록된 것과 미등록된 것 모두)
//     */
//    @GetMapping("/home")
//    @ResponseBody
//    public ResponseEntity<Map<String, Object>> getHomeCrops(
//            @AuthenticationPrincipal CustomUserDetails userDetails) {
//
//        Map<String, Object> response = new HashMap<>();
//
//        try {
//            User user = userService.findById(userDetails.getId());
//            List<HomeCropDto> homeCrops = cropService.getHomeCrops(user);
//
//            response.put("success", true);
//            response.put("crops", homeCrops);
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("홈 화면용 작물 목록 조회 실패", e);
//            response.put("success", false);
//            response.put("message", "작물 목록 조회에 실패했습니다.");
//            return ResponseEntity.badRequest().body(response);
//        }
//    }
//
//    /**
//     * 등록된 작물 목록 조회 (API)
//     */
//    @GetMapping("/registered")
//    @ResponseBody
//    public ResponseEntity<Map<String, Object>> getRegisteredCrops(
//            @AuthenticationPrincipal CustomUserDetails userDetails) {
//
//        Map<String, Object> response = new HashMap<>();
//
//        try {
//            User user = userService.findById(userDetails.getId());
//            List<Crop> registeredCrops = cropService.getUserRegisteredCrops(user);
//
//            response.put("success", true);
//            response.put("crops", registeredCrops);
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("등록된 작물 목록 조회 실패", e);
//            response.put("success", false);
//            response.put("message", "작물 목록 조회에 실패했습니다.");
//            return ResponseEntity.badRequest().body(response);
//        }
//    }
//
//    /**
//     * 작물 삭제
//     */
//    @DeleteMapping("/{cropId}")
//    @ResponseBody
//    public ResponseEntity<Map<String, Object>> deleteCrop(
//            @PathVariable Integer cropId,
//            @AuthenticationPrincipal CustomUserDetails userDetails) {
//
//        Map<String, Object> response = new HashMap<>();
//
//        try {
//            User user = userService.findById(userDetails.getId());
//            cropService.deleteCrop(cropId, user);
//
//            response.put("success", true);
//            response.put("message", "작물이 삭제되었습니다.");
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("작물 삭제 실패", e);
//            response.put("success", false);
//            response.put("message", "작물 삭제에 실패했습니다: " + e.getMessage());
//            return ResponseEntity.badRequest().body(response);
//        }
//    }
//
    /**
     * 작물 진단 페이지 정보 조회 (진단할 작물 정보 + 태그 선택 UI)
     */
    @GetMapping("/{cropId}/diagnosis")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCropDiagnosisPage(
            @PathVariable Integer cropId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> response = new HashMap<>();

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
            Map<String, Object> cropInfo = new HashMap<>();
            cropInfo.put("id", crop.getId());
            cropInfo.put("name", crop.getName());
            cropInfo.put("cropImg", crop.getCropImg());
            cropInfo.put("startAt", crop.getStartAt());
            cropInfo.put("endAt", crop.getEndAt());

            // 진단 태그 옵션들
            Map<String, String> diagnosisOptions = new HashMap<>();
            diagnosisOptions.put("CURRENT_STATUS", "현재상태분석");
            diagnosisOptions.put("DISEASE_CHECK", "질병여부");
            diagnosisOptions.put("QUALITY_MARKET", "시장성");

            response.put("success", true);
            response.put("crop", cropInfo);
            response.put("diagnosisOptions", diagnosisOptions);
            response.put("message", "진단할 태그를 선택해주세요.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("작물 진단 페이지 조회 실패", e);
            response.put("success", false);
            response.put("message", "정보 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 작물 진단 실행 (태그 선택 후)
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

            // 진단 수행
            CropDetailAnalysisResult result = cropService.analyzeCropDetail(crop, request.getAnalysisType());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("작물 진단 실패", e);
            return ResponseEntity.badRequest()
                .body(new CropDetailAnalysisResult(false, "진단에 실패했습니다: " + e.getMessage(), request.getAnalysisType()));
        }
    }
//
    /**
     * 진단 결과를 바탕으로 재배일지 작성 폼 데이터 조회
     */
    @PostMapping("/diagnosis-to-diary")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createDiaryWithDiagnosis(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> response = new HashMap<>();

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
            Map<String, Object> cropInfo = new HashMap<>();
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
}
