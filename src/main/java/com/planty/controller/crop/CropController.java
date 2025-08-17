package com.planty.controller.crop;

import com.planty.config.CustomUserDetails;
import com.planty.dto.crop.CropRegistrationDto;
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
}
