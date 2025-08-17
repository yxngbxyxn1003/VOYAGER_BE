package com.planty.controller.api;

import com.planty.config.CustomUserDetails;
import com.planty.dto.crop.CropDetailAnalysisResult;
import com.planty.entity.crop.AnalysisType;
import com.planty.entity.crop.Crop;
import com.planty.entity.user.User;
import com.planty.service.crop.CropService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 작물 분석 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/crop")
@RequiredArgsConstructor
public class CropAnalysisApiController {

    private final CropService cropService;

    /**
     * 작물 현재상태 분석
     */
    @PostMapping("/{cropId}/analyze/current-status")
    public ResponseEntity<CropDetailAnalysisResult> analyzeCropCurrentStatus(
            @PathVariable Integer cropId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        return analyzeCropDetailInternal(cropId, AnalysisType.CURRENT_STATUS, userDetails);
    }

    /**
     * 작물 질병여부 분석
     */
    @PostMapping("/{cropId}/analyze/disease-check")
    public ResponseEntity<CropDetailAnalysisResult> analyzeCropDiseaseCheck(
            @PathVariable Integer cropId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        return analyzeCropDetailInternal(cropId, AnalysisType.DISEASE_CHECK, userDetails);
    }

    /**
     * 작물 품질/시장성 분석
     */
    @PostMapping("/{cropId}/analyze/quality-market")
    public ResponseEntity<CropDetailAnalysisResult> analyzeCropQualityMarket(
            @PathVariable Integer cropId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        return analyzeCropDetailInternal(cropId, AnalysisType.QUALITY_MARKET, userDetails);
    }

    /**
     * 공통 분석 로직
     */
    private ResponseEntity<CropDetailAnalysisResult> analyzeCropDetailInternal(
            Integer cropId, AnalysisType analysisType, CustomUserDetails userDetails) {
        
        try {
            // 인증 확인
            if (userDetails == null) {
                return ResponseEntity.status(401)
                    .body(new CropDetailAnalysisResult(false, "인증이 필요합니다.", analysisType));
            }
            
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
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (IllegalArgumentException e) {
            log.warn("작물 세부 분석 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new CropDetailAnalysisResult(false, e.getMessage(), analysisType));
        } catch (Exception e) {
            log.error("작물 세부 분석 중 예상치 못한 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(new CropDetailAnalysisResult(false, "서버 내부 오류가 발생했습니다.", analysisType));
        }
    }
}
