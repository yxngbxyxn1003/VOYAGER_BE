package com.planty.config;

import com.theokanning.openai.service.OpenAiService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

/**
 * OpenAI 및 AI 모델 설정
 * DB에서 동적으로 로드되는 설정값들과 기본 OpenAI 서비스 관리
 */
@Configuration
@ConfigurationProperties(prefix = "ai")
@Getter @Setter
public class OpenApiConfig {

    @Value("${openai.api.key:}")
    private String apiKey;

    /**
     * OpenAI API 기본 설정
     */
    private OpenAI openai = new OpenAI();

    /**
     * 작물 등록 시 이미지 인식 및 재배방법 분석 설정
     */
    private CropRegistration cropRegistration = new CropRegistration();

    /**
     * 작물 상태 진단 설정 (현재상태, 질병여부, 품질/시장성)
     */
    private CropDiagnosis cropDiagnosis = new CropDiagnosis();

    @Bean
    public OpenAiService openAiService() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("OpenAI API key가 설정되지 않았습니다. application.yml에서 openai.api.key를 확인하세요.");
        }
        return new OpenAiService(apiKey, Duration.ofSeconds(openai.getTimeout()));
    }

    @Getter @Setter
    public static class OpenAI {
        private String baseUrl = "https://api.openai.com/v1";
        private int timeout = 60; // 초 단위
        private int maxRetries = 3;
        private boolean enableLogging = false;
    }

    @Getter @Setter
    public static class CropRegistration {
        private String model = "gpt-4o-mini";
        private int maxTokens = 1000;
        private double temperature = 0.1; // 일관된 결과를 위해 낮은 값
        private String systemPrompt = """
            당신은 농업 전문가입니다. 작물 이미지를 분석하여 정확한 작물 정보와 재배 방법을 제공해주세요.
            응답은 반드시 JSON 형식으로만 제공하고, 추가적인 설명은 포함하지 마세요.
            """;
        private String userPromptTemplate = """
            이 이미지의 작물을 분석해주세요. 다음 정보를 JSON 형태로 제공해주세요:
            1. 작물 이름 (cropName)
            2. 재배 환경 (environment) - 실내/실외/온실 등
            3. 적정 온도 (temperature) - 구체적인 온도 범위
            4. 예상 높이 (height) - 성장 후 예상 크기
            5. 키우는 방법 (howTo) - 상세한 재배 가이드

            응답은 반드시 다음과 같은 JSON 형식으로만 답변해주세요:
            {
              "cropName": "작물이름",
              "environment": "재배환경",
              "temperature": "적정온도",
              "height": "예상높이",
              "howTo": "상세한 키우는 방법"
            }
            """;
    }

    @Getter @Setter
    public static class CropDiagnosis {
        private String model = "gpt-4o"; // 더 정확한 진단을 위해 상위 모델 사용
        private int maxTokens = 1500;
        private double temperature = 0.2;
        
        // 현재 상태 진단 프롬프트
        private String currentStatusPrompt = """
            이 작물 이미지를 분석하여 현재 상태를 종합적으로 평가해주세요.
            전체적인 성장 상태, 건강도, 발달 정도 등을 포괄적으로 요약하여 JSON 형태로 제공해주세요.
            
            응답은 반드시 다음과 같은 JSON 형식으로만 답변해주세요:
            {
              "currentStatusSummary": "현재 상태 종합 분석 내용"
            }
            """;
        
        // 질병 진단 프롬프트
        private String diseaseCheckPrompt = """
            이 작물 이미지를 분석하여 질병 여부를 진단해주세요.
            병충해 감염 여부, 구체적인 질병명, 예방 및 치료 방법을 JSON 형태로 제공해주세요.
            
            응답은 반드시 다음과 같은 JSON 형식으로만 답변해주세요:
            {
              "diseaseStatus": "질병 상태 (건강함/경미한 질병/심각한 질병 등)",
              "diseaseDetails": "발견된 질병이나 문제점 상세 설명",
              "preventionMethods": "예방 및 치료 방법"
            }
            """;
        
        // 품질/시장성 분석 프롬프트
        private String qualityMarketPrompt = """
            이 작물 이미지를 분석하여 품질과 시장성을 평가해주세요.
            출하시 상품 비율, 색상 품질, 맛과 저장성, 운송 저항성 등을 JSON 형태로 제공해주세요.
            
            응답은 반드시 다음과 같은 JSON 형식으로만 답변해주세요:
            {
              "marketRatio": "출하시 상품 비율 평가",
              "colorUniformity": "색 균일도 평가",
              "saturation": "채도 평가",
              "brightness": "명도 평가",
              "tasteStorage": "맛과 저장성 평가",
              "transportResistance": "운송 저장 중 손상 저항성 평가",
              "storageEvaluation": "저장성 종합 평가"
            }
            """;
    }

    /**
     * 동적 설정 업데이트 메서드 (DB에서 로드된 값으로 업데이트)
     */
    public void updateFromDatabase(Map<String, Object> configValues) {
        // OpenAI 설정 업데이트
        updateOpenAISettings(configValues);
        
        // 작물 등록 설정 업데이트
        updateCropRegistrationSettings(configValues);
        
        // 작물 진단 설정 업데이트
        updateCropDiagnosisSettings(configValues);
    }

    private void updateOpenAISettings(Map<String, Object> configValues) {
        if (configValues.containsKey("openai.timeout")) {
            openai.setTimeout((Integer) configValues.get("openai.timeout"));
        }
        if (configValues.containsKey("openai.maxRetries")) {
            openai.setMaxRetries((Integer) configValues.get("openai.maxRetries"));
        }
        if (configValues.containsKey("openai.enableLogging")) {
            openai.setEnableLogging((Boolean) configValues.get("openai.enableLogging"));
        }
    }

    private void updateCropRegistrationSettings(Map<String, Object> configValues) {
        if (configValues.containsKey("crop.registration.model")) {
            cropRegistration.setModel((String) configValues.get("crop.registration.model"));
        }
        if (configValues.containsKey("crop.registration.temperature")) {
            cropRegistration.setTemperature((Double) configValues.get("crop.registration.temperature"));
        }
        if (configValues.containsKey("crop.registration.maxTokens")) {
            cropRegistration.setMaxTokens((Integer) configValues.get("crop.registration.maxTokens"));
        }
    }

    private void updateCropDiagnosisSettings(Map<String, Object> configValues) {
        if (configValues.containsKey("crop.diagnosis.model")) {
            cropDiagnosis.setModel((String) configValues.get("crop.diagnosis.model"));
        }
        if (configValues.containsKey("crop.diagnosis.temperature")) {
            cropDiagnosis.setTemperature((Double) configValues.get("crop.diagnosis.temperature"));
        }
        if (configValues.containsKey("crop.diagnosis.maxTokens")) {
            cropDiagnosis.setMaxTokens((Integer) configValues.get("crop.diagnosis.maxTokens"));
        }
    }
}
