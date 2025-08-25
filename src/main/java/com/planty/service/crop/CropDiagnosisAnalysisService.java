package com.planty.service.crop;

import com.planty.dto.crop.CropDetailAnalysisResult;
import com.planty.entity.crop.AnalysisType;
import com.planty.entity.crop.Crop;
import com.planty.service.openai.OpenAIService;
import com.planty.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 작물 태그별 진단 분석 전용 서비스
 * - 등록된 작물의 현재상태, 질병여부, 품질/시장성 등 세부 진단
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CropDiagnosisAnalysisService {

    private final OpenAIService openAIService;
    private final StorageService storageService;

    /**
     * 작물 상세페이지에서 진단받기 (해당 cropID로 진단 진행)
     */
    public CropDetailAnalysisResult analyzeCropDiagnosis(Integer cropId, com.planty.entity.user.User user, AnalysisType analysisType, MultipartFile image) throws IOException {
        // 이미지 검증 제거 - 어떤 이미지든 허용

        log.info("작물 진단 시작 - 사용자: {}, 작물ID: {}, 분석타입: {}, 이미지: {} ({} bytes)", 
            user.getNickname(), cropId, analysisType, image.getOriginalFilename(), image.getSize());

        // 진단 분석 타입인지 확인
        if (!analysisType.isDiagnosisAnalysis()) {
            return new CropDetailAnalysisResult(false, "잘못된 분석 타입입니다. 진단 분석만 가능합니다.", analysisType);
        }

        String savedImagePath = null;
        try {
            // 이미지 파일 저장
            savedImagePath = storageService.save(image, "diagnosis");
            log.info("진단 이미지 저장 완료: {}", savedImagePath);

            // OpenAI로 진단 분석 수행
            CropDetailAnalysisResult result = openAIService.analyzeCropDetail(savedImagePath, analysisType);
            
            if (result.isSuccess()) {
                log.info("작물 진단 완료 - 사용자: {}, 작물ID: {}, 분석타입: {}", 
                    user.getNickname(), cropId, analysisType);
            } else {
                log.error("작물 진단 실패 - 사용자: {}, 작물ID: {}, 분석타입: {}, 오류: {}", 
                    user.getNickname(), cropId, analysisType, result.getMessage());
            }

            return result;

        } catch (Exception e) {
            log.error("작물 진단 중 오류 발생 - 사용자: {}, 작물ID: {}, 분석타입: {}, 이미지: {}, 오류: {}", 
                user.getNickname(), cropId, analysisType, image.getOriginalFilename(), e.getMessage(), e);
            
            // 저장된 이미지가 있으면 삭제 시도
            if (savedImagePath != null) {
                try {
                    storageService.deleteByUrl(savedImagePath);
                    log.info("오류 발생으로 인한 이미지 파일 삭제 완료: {}", savedImagePath);
                } catch (Exception deleteEx) {
                    log.warn("오류 발생 후 이미지 파일 삭제 실패: {}, 오류: {}", savedImagePath, deleteEx.getMessage());
                }
            }
            
            return new CropDetailAnalysisResult(false, "진단 분석 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", analysisType);
        }
    }

    /**
     *
     * 작물 상세페이지에서 cropID 기반 진단을 사용
     */
    @Deprecated
    public CropDetailAnalysisResult analyzeCropDiagnosisStandalone(com.planty.entity.user.User user, AnalysisType analysisType, MultipartFile image) throws IOException {
        try {
            // 진단 분석 타입인지 확인
            if (!analysisType.isDiagnosisAnalysis()) {
                return new CropDetailAnalysisResult(false, "잘못된 분석 타입입니다. 진단 분석만 가능합니다.", analysisType);
            }

            log.info("독립적인 작물 진단 시작 - 사용자: {}, 분석 타입: {}, 파일: {} (크기: {} bytes)", 
                    user.getNickname(), analysisType, image.getOriginalFilename(), image.getSize());

            // 이미지 파일을 임시로 저장
            String savedImagePath = storageService.save(image, "diagnosis");

            // OpenAI를 통한 태그별 진단 분석
            CropDetailAnalysisResult result = openAIService.analyzeCropDetail(savedImagePath, analysisType);

            // 임시 이미지 파일 삭제
            storageService.deleteByUrl(savedImagePath);

            if (result.isSuccess()) {
                log.info("독립적인 작물 진단 완료 - 사용자: {}, 분석 타입: {}, 성공: {}", 
                        user.getNickname(), analysisType, result.isSuccess());
            } else {
                log.warn("독립적인 작물 진단 실패 - 사용자: {}, 분석 타입: {}, 메시지: {}", 
                        user.getNickname(), analysisType, result.getMessage());
            }

            return result;

        } catch (Exception e) {
            log.error("독립적인 작물 진단 중 오류 발생 - 사용자: {}, 분석 타입: {}, 파일: {}", 
                    user.getNickname(), analysisType, image.getOriginalFilename(), e);
            return new CropDetailAnalysisResult(false, "진단 분석 중 오류가 발생했습니다: " + e.getMessage(), analysisType);
        }
    }
}
