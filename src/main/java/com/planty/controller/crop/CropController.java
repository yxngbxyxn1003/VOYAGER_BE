package com.planty.controller.crop;

import com.planty.config.CustomUserDetails;
import com.planty.dto.crop.CropRegistrationDto;
import com.planty.dto.crop.CropDetailAnalysisResult;
import com.planty.dto.crop.HomeCropDto;
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

    /**
     * 작물 목록 페이지
     */
    @GetMapping
    public String cropList(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userDetails.getUser();
        List<Crop> crops = cropService.getUserCrops(user);
        
        model.addAttribute("crops", crops);
        return "crop/crop-list";
    }

    /**
     * 작물 이미지 업로드 (AJAX)
     */
    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadCropImage(
            @RequestParam("imageFile") MultipartFile imageFile,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = userDetails.getUser();
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
            User user = userDetails.getUser();
            
            // 권한 확인
            if (!crop.getUser().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("message", "권한이 없습니다.");
                return ResponseEntity.forbidden().body(response);
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

    /**
     * 작물 등록 완료 (이름, 날짜 입력 후)
     */
    @PostMapping("/complete-registration")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> completeCropRegistration(
            @RequestBody CropRegistrationDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = userDetails.getUser();
            Crop crop = cropService.getCropById(dto.getId());
            
            // 권한 확인
            if (!crop.getUser().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("message", "권한이 없습니다.");
                return ResponseEntity.forbidden().body(response);
            }
            
            Crop completedCrop = cropService.completeCropRegistration(dto.getId(), dto);
            
            response.put("success", true);
            response.put("message", "작물 등록이 완료되었습니다.");
            response.put("crop", completedCrop);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("작물 등록 완료 실패", e);
            response.put("success", false);
            response.put("message", "작물 등록에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 작물 상세 정보 조회
     */
    @GetMapping("/{cropId}")
    public String cropDetail(@PathVariable Integer cropId, 
                           @AuthenticationPrincipal CustomUserDetails userDetails, 
                           Model model) {
        try {
            Crop crop = cropService.getCropById(cropId);
            User user = userDetails.getUser();
            
            // 권한 확인
            if (!crop.getUser().getId().equals(user.getId())) {
                model.addAttribute("error", "권한이 없습니다.");
                return "error/403";
            }
            
            model.addAttribute("crop", crop);
            return "crop/crop-detail";
            
        } catch (Exception e) {
            log.error("작물 상세 조회 실패", e);
            model.addAttribute("error", "작물 정보를 찾을 수 없습니다.");
            return "error/404";
        }
    }

    /**
     * 홈 화면용 작물 목록 조회 (등록된 것과 미등록된 것 모두)
     */
    @GetMapping("/home")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getHomeCrops(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = userDetails.getUser();
            List<HomeCropDto> homeCrops = cropService.getHomeCrops(user);
            
            response.put("success", true);
            response.put("crops", homeCrops);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("홈 화면용 작물 목록 조회 실패", e);
            response.put("success", false);
            response.put("message", "작물 목록 조회에 실패했습니다.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 등록된 작물 목록 조회 (API)
     */
    @GetMapping("/registered")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRegisteredCrops(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = userDetails.getUser();
            List<Crop> registeredCrops = cropService.getUserRegisteredCrops(user);
            
            response.put("success", true);
            response.put("crops", registeredCrops);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("등록된 작물 목록 조회 실패", e);
            response.put("success", false);
            response.put("message", "작물 목록 조회에 실패했습니다.");
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
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = userDetails.getUser();
            cropService.deleteCrop(cropId, user);
            
            response.put("success", true);
            response.put("message", "작물이 삭제되었습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("작물 삭제 실패", e);
            response.put("success", false);
            response.put("message", "작물 삭제에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 작물 세부 분석 (현재상태, 질병여부, 품질/시장성)
     */
    @PostMapping("/analyze-detail/{cropId}")
    @ResponseBody
    public ResponseEntity<CropDetailAnalysisResult> analyzeCropDetail(
            @PathVariable Integer cropId,
            @RequestParam("analysisType") AnalysisType analysisType,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            User user = userDetails.getUser();
            Crop crop = cropService.getCropById(cropId);
            
            // 권한 확인
            if (!crop.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403)
                    .body(new CropDetailAnalysisResult(false, "권한이 없습니다.", analysisType));
            }
            
            // 등록된 작물만 세부 분석 가능
            if (!crop.getIsRegistered()) {
                return ResponseEntity.badRequest()
                    .body(new CropDetailAnalysisResult(false, "등록되지 않은 작물은 분석할 수 없습니다.", analysisType));
            }
            
            // 세부 분석 수행
            CropDetailAnalysisResult result = cropService.analyzeCropDetail(crop, analysisType);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("작물 세부 분석 실패", e);
            return ResponseEntity.badRequest()
                .body(new CropDetailAnalysisResult(false, "분석에 실패했습니다: " + e.getMessage(), analysisType));
        }
    }

    /**
     * 작물 분석 완료 후 재배일지 작성 폼으로 이동 (AI 분석 결과 포함)
     */
    @GetMapping("/{cropId}/create-diary")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createDiaryWithCropAnalysis(
            @PathVariable Integer cropId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = userDetails.getUser();
            Crop crop = cropService.getCropById(cropId);
            
            // 권한 확인
            if (!crop.getUser().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("message", "권한이 없습니다.");
                return ResponseEntity.forbidden().body(response);
            }
            
            // 분석이 완료된 작물만 가능
            if (crop.getAnalysisStatus() != AnalysisStatus.COMPLETED) {
                response.put("success", false);
                response.put("message", "분석이 완료되지 않은 작물입니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 작물 정보와 AI 분석 결과를 포함한 응답
            Map<String, Object> cropInfo = new HashMap<>();
            cropInfo.put("id", crop.getId());
            cropInfo.put("name", crop.getName());
            cropInfo.put("cropImg", crop.getCropImg());
            
            // AI 분석 결과
            Map<String, String> analysisResult = new HashMap<>();
            analysisResult.put("environment", crop.getEnvironment());
            analysisResult.put("temperature", crop.getTemperature());
            analysisResult.put("height", crop.getHeight());
            analysisResult.put("howTo", crop.getHowTo());
            
            response.put("success", true);
            response.put("crop", cropInfo);
            response.put("analysisResult", analysisResult);
            response.put("message", "재배일지 작성 정보를 가져왔습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("재배일지 작성 정보 조회 실패", e);
            response.put("success", false);
            response.put("message", "정보 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
