package com.planty.service.crop;

import com.planty.dto.crop.CropDetailAnalysisResult;
import com.planty.entity.crop.AnalysisType;
import com.planty.entity.crop.Crop;
import com.planty.service.openai.OpenAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * 작물 태그별 진단 분석 전용 서비스
 * - 등록된 작물의 현재상태, 질병여부, 품질/시장성 등 세부 진단
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CropDiagnosisAnalysisService {

    private final OpenAIService openAIService;

    /**
     * 작물 태그별 진단 분석 (현재상태, 질병여부, 품질/시장성)
     */
    public CropDetailAnalysisResult analyzeCropDetail(Crop crop, AnalysisType analysisType) {
        try {
            // 진단 분석 타입인지 확인
            if (!analysisType.isDiagnosisAnalysis()) {
                return new CropDetailAnalysisResult(false, "잘못된 분석 타입입니다. 진단 분석만 가능합니다.", analysisType);
            }

            // 작물 이미지가 없는 경우
            if (crop.getCropImg() == null || crop.getCropImg().trim().isEmpty()) {
                return new CropDetailAnalysisResult(false, "분석할 이미지가 없습니다.", analysisType);
            }

            // 이미지 파일 존재 확인
            File imageFile = new File(crop.getCropImg());
            if (!imageFile.exists()) {
                return new CropDetailAnalysisResult(false, "이미지 파일을 찾을 수 없습니다.", analysisType);
            }

            // OpenAI를 통한 태그별 진단 분석
            CropDetailAnalysisResult result = openAIService.analyzeCropDetail(crop.getCropImg(), analysisType);

            log.info("작물 태그별 진단 분석 완료 - 작물 ID: {}, 분석 타입: {}, 성공: {}",
                    crop.getId(), analysisType, result.isSuccess());

            return result;

        } catch (Exception e) {
            log.error("작물 태그별 진단 분석 중 오류 발생 - 작물 ID: {}, 분석 타입: {}", crop.getId(), analysisType, e);
            return new CropDetailAnalysisResult(false, "진단 분석 중 오류가 발생했습니다: " + e.getMessage(), analysisType);
        }
    }
}
