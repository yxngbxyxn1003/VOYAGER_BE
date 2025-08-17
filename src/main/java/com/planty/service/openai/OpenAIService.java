package com.planty.service.openai;

import com.planty.dto.crop.CropAnalysisResult;
import com.planty.dto.crop.CropDetailAnalysisResult;
import com.planty.entity.crop.AnalysisType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Base64;

@Slf4j
@Service
public class OpenAIService {

    @Value("${openai.api.key:}")
    private String apiKey;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAIService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 작물 이미지를 분석하여 작물 정보를 반환
     */
    public CropAnalysisResult analyzeCropImage(String imagePath) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("OpenAI API key가 설정되지 않았습니다.");
            return new CropAnalysisResult(false, "API 키가 설정되지 않았습니다.");
        }

        try {
            // 이미지 파일을 Base64로 인코딩
            String base64Image = encodeImageToBase64(imagePath);
            
            // OpenAI Vision API 호출
            String requestBody = createVisionApiRequestBody(base64Image);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return parseVisionApiResponse(response.body());
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

    private String createVisionApiRequestBody(String base64Image) {
        return String.format("""
            {
                "model": "gpt-4o-mini",
                "messages": [
                    {
                        "role": "user",
                        "content": [
                            {
                                "type": "text",
                                "text": "이 이미지의 작물을 분석해주세요. 다음 정보를 JSON 형태로 제공해주세요:\\n1. 작물 이름 (cropName)\\n2. 재배 환경 (environment)\\n3. 적정 온도 (temperature)\\n4. 예상 높이 (height)\\n5. 키우는 방법 (howTo)\\n\\n응답은 반드시 다음과 같은 JSON 형식으로만 답변해주세요:\\n{\\n  \\"cropName\\": \\"작물이름\\",\\n  \\"environment\\": \\"재배환경\\",\\n  \\"temperature\\": \\"적정온도\\",\\n  \\"height\\": \\"예상높이\\",\\n  \\"howTo\\": \\"상세한 키우는 방법\\"\\n}"
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
                "max_tokens": 1000
            }
            """, base64Image);
    }

    private CropAnalysisResult parseVisionApiResponse(String responseBody) {
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
                result.setCropName(analysisResult.get("cropName").asText());
                result.setEnvironment(analysisResult.get("environment").asText());
                result.setTemperature(analysisResult.get("temperature").asText());
                result.setHeight(analysisResult.get("height").asText());
                result.setHowTo(analysisResult.get("howTo").asText());
                result.setAnalysisMessage("이미지 분석이 완료되었습니다.");
                
                return result;
            }
            
        } catch (Exception e) {
            log.error("OpenAI 응답 파싱 중 오류 발생", e);
        }
        
        return new CropAnalysisResult(false, "응답 파싱에 실패했습니다.");
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
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("OpenAI API key가 설정되지 않았습니다.");
            return new CropDetailAnalysisResult(false, "API 키가 설정되지 않았습니다.", analysisType);
        }

        try {
            // 이미지 파일을 Base64로 인코딩
            String base64Image = encodeImageToBase64(imagePath);
            
            // 분석 타입별 요청 바디 생성
            String requestBody = createDetailAnalysisRequestBody(base64Image, analysisType);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return parseDetailAnalysisResponse(response.body(), analysisType);
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
     * 분석 타입별 요청 바디 생성
     */
    private String createDetailAnalysisRequestBody(String base64Image, AnalysisType analysisType) {
        String prompt = getPromptByAnalysisType(analysisType);
        
        return String.format("""
            {
                "model": "gpt-4o-mini",
                "messages": [
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
                "max_tokens": 1500
            }
            """, prompt, base64Image);
    }

    /**
     * 분석 타입별 프롬프트 반환
     */
    private String getPromptByAnalysisType(AnalysisType analysisType) {
        return switch (analysisType) {
            case CURRENT_STATUS -> 
                "이 작물 이미지를 분석하여 현재 상태를 종합적으로 평가해주세요. " +
                "전체적인 성장 상태, 건강도, 발달 정도 등을 포괄적으로 요약하여 JSON 형태로 제공해주세요.\\n" +
                "응답은 반드시 다음과 같은 JSON 형식으로만 답변해주세요:\\n" +
                "{\\n" +
                "  \\"currentStatusSummary\\": \\"현재 상태 종합 분석 내용\\"\\n" +
                "}";
                
            case DISEASE_CHECK ->
                "이 작물 이미지를 분석하여 질병 여부를 진단해주세요. " +
                "병충해 감염 여부, 구체적인 질병명, 예방 및 치료 방법을 JSON 형태로 제공해주세요.\\n" +
                "응답은 반드시 다음과 같은 JSON 형식으로만 답변해주세요:\\n" +
                "{\\n" +
                "  \\"diseaseStatus\\": \\"질병 상태 (건강함/경미한 질병/심각한 질병 등)\\",\\n" +
                "  \\"diseaseDetails\\": \\"발견된 질병이나 문제점 상세 설명\\",\\n" +
                "  \\"preventionMethods\\": \\"예방 및 치료 방법\\"\\n" +
                "}";
                
            case QUALITY_MARKET ->
                "이 작물 이미지를 분석하여 품질과 시장성을 평가해주세요. " +
                "출하시 상품 비율, 색상 품질, 맛과 저장성, 운송 저항성 등을 JSON 형태로 제공해주세요.\\n" +
                "응답은 반드시 다음과 같은 JSON 형식으로만 답변해주세요:\\n" +
                "{\\n" +
                "  \\"marketRatio\\": \\"출하시 상품 비율 평가\\",\\n" +
                "  \\"colorUniformity\\": \\"색 균일도 평가\\",\\n" +
                "  \\"saturation\\": \\"채도 평가\\",\\n" +
                "  \\"brightness\\": \\"명도 평가\\",\\n" +
                "  \\"tasteStorage\\": \\"맛과 저장성 평가\\",\\n" +
                "  \\"transportResistance\\": \\"운송 저장 중 손상 저항성 평가\\",\\n" +
                "  \\"storageEvaluation\\": \\"저장성 종합 평가\\"\\n" +
                "}";
        };
    }

    /**
     * 세부 분석 응답 파싱
     */
    private CropDetailAnalysisResult parseDetailAnalysisResponse(String responseBody, AnalysisType analysisType) {
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
                        result.setCurrentStatusSummary(analysisResult.get("currentStatusSummary").asText());
                    }
                    case DISEASE_CHECK -> {
                        result.setDiseaseStatus(analysisResult.get("diseaseStatus").asText());
                        result.setDiseaseDetails(analysisResult.get("diseaseDetails").asText());
                        result.setPreventionMethods(analysisResult.get("preventionMethods").asText());
                    }
                    case QUALITY_MARKET -> {
                        result.setMarketRatio(analysisResult.get("marketRatio").asText());
                        result.setColorUniformity(analysisResult.get("colorUniformity").asText());
                        result.setSaturation(analysisResult.get("saturation").asText());
                        result.setBrightness(analysisResult.get("brightness").asText());
                        result.setTasteStorage(analysisResult.get("tasteStorage").asText());
                        result.setTransportResistance(analysisResult.get("transportResistance").asText());
                        result.setStorageEvaluation(analysisResult.get("storageEvaluation").asText());
                    }
                }
                
                return result;
            }
            
        } catch (Exception e) {
            log.error("OpenAI 세부 분석 응답 파싱 중 오류 발생", e);
        }
        
        return new CropDetailAnalysisResult(false, "응답 파싱에 실패했습니다.", analysisType);
    }
}
