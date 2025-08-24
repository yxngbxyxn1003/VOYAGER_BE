package com.planty.service.openai;

import com.planty.config.OpenApiConfig;
import com.planty.config.ParameterStoreConfig;
import com.planty.dto.crop.CropAnalysisResult;
import com.planty.dto.crop.CropDetailAnalysisResult;
import com.planty.entity.crop.AnalysisType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIService {

    private final OpenApiConfig openApiConfig;
    private final ParameterStoreConfig parameterStoreConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${openai.api.key:}")
    private String fallbackApiKey;

    private HttpClient httpClient;

    /**
     * 지연 초기화를 통한 HttpClient 생성
     */
    private HttpClient getHttpClient() {
        if (httpClient == null) {
            int timeout = openApiConfig.getOpenai().getTimeout();
            httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeout))
                    .build();
        }
        return httpClient;
    }

    /**
     * API 키 조회 (Parameter Store > 환경변수 > 설정파일 순서)
     */
    private String getApiKey() {
        // 1. Parameter Store에서 조회
        String apiKey = parameterStoreConfig.getOpenAIApiKey();
        
        // 2. 환경변수 폴백
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = System.getenv("OPENAI_API_KEY");
        }
        
        // 3. 설정파일 폴백
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = fallbackApiKey;
        }
        
        return apiKey;
    }

    /**
     * 작물 이미지를 분석하여 작물 정보를 반환 (작물 등록용)
     */
    public CropAnalysisResult analyzeCropImage(String imagePath) {
        return analyzeCropImage(imagePath, AnalysisType.REGISTRATION_ANALYSIS);
    }

    /**
     * 작물 이미지를 분석하여 작물 정보를 반환 (분석 타입 지정)
     */
    public CropAnalysisResult analyzeCropImage(String imagePath, AnalysisType analysisType) {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("OpenAI API key가 설정되지 않았습니다.");
            return new CropAnalysisResult(false, "API 키가 설정되지 않았습니다.");
        }

        try {
            // 이미지 파일을 Base64로 인코딩
            String base64Image = encodeImageToBase64(imagePath);

            // 작물 등록용 요청 바디 생성
            String requestBody = createCropRegistrationRequestBody(base64Image);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(openApiConfig.getOpenai().getBaseUrl() + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(openApiConfig.getOpenai().getTimeout()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseCropAnalysisResponse(response.body());
            } else {
                log.error("OpenAI API 호출 실패: {} - {}", response.statusCode(), response.body());
                return new CropAnalysisResult(false, "이미지 분석에 실패했습니다.");
            }

        } catch (Exception e) {
            log.error("이미지 분석 중 오류 발생", e);
            return new CropAnalysisResult(false, "이미지 분석 중 오류가 발생했습니다.");
        }
    }

    private String encodeImageToBase64(String imagePath) throws IOException {
        File imageFile = new File(imagePath);
        byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * 작물 등록용 요청 바디 생성
     */
    private String createCropRegistrationRequestBody(String base64Image) {
        OpenApiConfig.CropRegistration config = openApiConfig.getCropRegistration();
        
        // JSON 이스케이프 처리
        String escapedSystemPrompt = escapeJsonString(config.getSystemPrompt());
        String escapedUserPrompt = escapeJsonString(config.getUserPromptTemplate());
        
        return String.format("""
            {
                "model": "%s",
                "messages": [
                    {
                        "role": "system",
                        "content": "%s"
                    },
                    {
                        "role": "user",
                        "content": [
                            {
                                "type": "text",
                                "text": "%s"
                            },
                            {
                                "type": "image_url",
                                "image_url": {
                                    "url": "data:image/jpeg;base64,%s"
                                }
                            }
                        ]
                    }
                ],
                "max_tokens": %d,
                "temperature": %.1f
            }
            """, 
            config.getModel(),
            escapedSystemPrompt,
            escapedUserPrompt,
            base64Image,
            config.getMaxTokens(),
            config.getTemperature()
        );
    }
    
    /**
     * JSON 문자열 이스케이프 처리
     */
    private String escapeJsonString(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * 작물 분석 응답 파싱
     */
    private CropAnalysisResult parseCropAnalysisResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");

            if (choices != null && choices.size() > 0) {
                String content = choices.get(0).get("message").get("content").asText();

                // JSON 응답에서 실제 JSON 부분만 추출
                String jsonContent = extractJsonFromContent(content);
                JsonNode analysisResult = objectMapper.readTree(jsonContent);

                CropAnalysisResult result = new CropAnalysisResult();
                result.setSuccess(true);
                result.setCropName(getSafeText(analysisResult, "cropName"));
                result.setEnvironment(getSafeText(analysisResult, "environment"));
                result.setTemperature(getSafeText(analysisResult, "temperature"));
                result.setHeight(getSafeText(analysisResult, "height"));
                result.setHowTo(getSafeText(analysisResult, "howTo"));
                result.setAnalysisMessage("작물 이미지 분석이 완료되었습니다.");

                // 로깅 (디버그용)
                if (openApiConfig.getOpenai().isEnableLogging()) {
                    log.info("작물 분석 결과: {}", result);
                }

                return result;
            }

        } catch (Exception e) {
            log.error("OpenAI 응답 파싱 중 오류 발생", e);
        }

        return new CropAnalysisResult(false, "응답 파싱에 실패했습니다.");
    }

    /**
     * JSON 노드에서 안전하게 텍스트 추출
     */
    private String getSafeText(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null ? fieldNode.asText() : "분석 중";
    }

    private String extractJsonFromContent(String content) {
        // JSON 부분만 추출 (```json 태그나 다른 텍스트 제거)
        int startIndex = content.indexOf("{");
        int endIndex = content.lastIndexOf("}") + 1;

        if (startIndex >= 0 && endIndex > startIndex) {
            return content.substring(startIndex, endIndex);
        }

        return content;
    }

    /**
     * 작물 세부 분석 (현재상태, 질병여부, 품질/시장성)
     */
    public CropDetailAnalysisResult analyzeCropDetail(String imagePath, AnalysisType analysisType) {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("OpenAI API key가 설정되지 않았습니다.");
            return new CropDetailAnalysisResult(false, "API 키가 설정되지 않았습니다.", analysisType);
        }

        try {
            // 이미지 파일을 Base64로 인코딩
            String base64Image = encodeImageToBase64(imagePath);

            // 분석 타입별 요청 바디 생성
            String requestBody = createDiagnosisRequestBody(base64Image, analysisType);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(openApiConfig.getOpenai().getBaseUrl() + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(openApiConfig.getOpenai().getTimeout()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseDiagnosisResponse(response.body(), analysisType);
            } else {
                log.error("OpenAI API 호출 실패: {} - {}", response.statusCode(), response.body());
                return new CropDetailAnalysisResult(false, "이미지 분석에 실패했습니다.", analysisType);
            }

        } catch (Exception e) {
            log.error("이미지 세부 분석 중 오류 발생", e);
            return new CropDetailAnalysisResult(false, "이미지 분석 중 오류가 발생했습니다.", analysisType);
        }
    }

    /**
     * 진단용 요청 바디 생성
     */
    private String createDiagnosisRequestBody(String base64Image, AnalysisType analysisType) {
        OpenApiConfig.CropDiagnosis config = openApiConfig.getCropDiagnosis();
        String prompt = getPromptByAnalysisType(analysisType);

        return String.format("""
            {
                "model": "%s",
                "messages": [
                    {
                        "role": "system",
                        "content": "당신은 농업 전문가입니다. 작물 이미지를 정확히 분석하여 요청된 진단 정보를 JSON 형식으로 제공해주세요."
                    },
                    {
                        "role": "user",
                        "content": [
                            {
                                "type": "text",
                                "text": "%s"
                            },
                            {
                                "type": "image_url",
                                "image_url": {
                                    "url": "data:image/jpeg;base64,%s"
                                }
                            }
                        ]
                    }
                ],
                "max_tokens": %d,
                "temperature": %.1f
            }
            """, 
            config.getModel(),
            prompt,
            base64Image,
            config.getMaxTokens(),
            config.getTemperature()
        );
    }

    /**
     * 분석 타입별 프롬프트 반환
     */
    private String getPromptByAnalysisType(AnalysisType analysisType) {
        OpenApiConfig.CropDiagnosis config = openApiConfig.getCropDiagnosis();
        
        return switch (analysisType) {
            case CURRENT_STATUS -> config.getCurrentStatusPrompt();
            case DISEASE_CHECK -> config.getDiseaseCheckPrompt();
            case QUALITY_MARKET -> config.getQualityMarketPrompt();
            case REGISTRATION_ANALYSIS -> throw new IllegalArgumentException("재배방법 분석은 이 메서드에서 지원하지 않습니다.");
        };
    }

    /**
     * 진단 응답 파싱
     */
    private CropDetailAnalysisResult parseDiagnosisResponse(String responseBody, AnalysisType analysisType) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");

            if (choices != null && choices.size() > 0) {
                String content = choices.get(0).get("message").get("content").asText();

                // JSON 응답에서 실제 JSON 부분만 추출
                String jsonContent = extractJsonFromContent(content);
                JsonNode analysisResult = objectMapper.readTree(jsonContent);

                CropDetailAnalysisResult result = new CropDetailAnalysisResult();
                result.setSuccess(true);
                result.setAnalysisType(analysisType);
                result.setMessage("분석이 완료되었습니다.");

                // 분석 타입별로 결과 매핑
                switch (analysisType) {
                    case CURRENT_STATUS -> {
                        result.setCurrentStatusSummary(getSafeText(analysisResult, "currentStatusSummary"));
                    }
                    case DISEASE_CHECK -> {
                        result.setDiseaseStatus(getSafeText(analysisResult, "diseaseStatus"));
                        result.setDiseaseDetails(getSafeText(analysisResult, "diseaseDetails"));
                        result.setPreventionMethods(getSafeText(analysisResult, "preventionMethods"));
                    }
                    case QUALITY_MARKET -> {
                        result.setMarketRatio(getSafeText(analysisResult, "marketRatio"));
                        result.setColorUniformity(getSafeText(analysisResult, "colorUniformity"));
                        result.setSaturation(getSafeText(analysisResult, "saturation"));
                        result.setBrightness(getSafeText(analysisResult, "brightness"));
                        result.setTasteStorage(getSafeText(analysisResult, "tasteStorage"));
                        result.setTransportResistance(getSafeText(analysisResult, "transportResistance"));
                        result.setStorageEvaluation(getSafeText(analysisResult, "storageEvaluation"));
                    }
                }

                // 로깅 (디버그용)
                if (openApiConfig.getOpenai().isEnableLogging()) {
                    log.info("작물 진단 결과 - 타입: {}, 결과: {}", analysisType, result);
                }

                return result;
            }

        } catch (Exception e) {
            log.error("OpenAI 진단 응답 파싱 중 오류 발생", e);
        }

        return new CropDetailAnalysisResult(false, "응답 파싱에 실패했습니다.", analysisType);
    }

}
