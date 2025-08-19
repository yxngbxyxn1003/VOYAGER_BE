package com.planty.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 배포용 파라미터 스토어 설정
 * AWS Parameter Store, Azure Key Vault, Google Secret Manager 등을 통한 보안 키 관리
 */
@Slf4j
@Configuration
@Profile("!local") // local 프로파일이 아닐 때만 활성화
public class ParameterStoreConfig {

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${parameter-store.enabled:false}")
    private boolean parameterStoreEnabled;

    @Value("${parameter-store.region:us-east-1}")
    private String region;

    @Value("${parameter-store.prefix:/planty}")
    private String parameterPrefix;

    // 메모리 캐시 (실제 운영에서는 Redis 등 사용 권장)
    private final Map<String, String> parameterCache = new HashMap<>();

    @PostConstruct
    public void initializeParameterStore() {
        if (!parameterStoreEnabled) {
            log.info("Parameter Store가 비활성화되어 있습니다. 로컬 설정을 사용합니다.");
            return;
        }

        log.info("Parameter Store 초기화 중... Profile: {}, Region: {}", activeProfile, region);
        
        try {
            // 실제 구현에서는 여기서 AWS SDK, Azure SDK 등을 사용하여 파라미터를 로드
            loadParametersFromStore();
            log.info("Parameter Store 초기화 완료");
        } catch (Exception e) {
            log.error("Parameter Store 초기화 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Parameter Store 초기화 실패", e);
        }
    }

    /**
     * Parameter Store에서 파라미터 로드
     * 실제 구현에서는 AWS Systems Manager Parameter Store, Azure Key Vault 등 사용
     */
    private void loadParametersFromStore() {
        // 실제 배포 시 AWS Parameter Store에서 API 키 조회
        // 예시 파라미터들 (실제로는 외부 서비스에서 로드)
        Map<String, String> parameters = getParametersFromExternalStore();
        
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            parameterCache.put(entry.getKey(), entry.getValue());
            log.debug("Parameter loaded: {}", entry.getKey());
        }
    }

    /**
     * 외부 파라미터 스토어에서 파라미터 조회
     * 실제 구현 시 각 클라우드 서비스 SDK 사용
     */
    private Map<String, String> getParametersFromExternalStore() {
        Map<String, String> parameters = new HashMap<>();
        
        // AWS Parameter Store 사용 예시 (주석)
        /*
        try (SsmClient ssmClient = SsmClient.builder().region(Region.of(region)).build()) {
            GetParametersByPathRequest request = GetParametersByPathRequest.builder()
                    .path(parameterPrefix)
                    .recursive(true)
                    .withDecryption(true)
                    .build();
                    
            GetParametersByPathResponse response = ssmClient.getParametersByPath(request);
            
            for (Parameter parameter : response.parameters()) {
                String key = parameter.name().replace(parameterPrefix + "/", "");
                parameters.put(key, parameter.value());
            }
        }
        */

        // 개발/테스트용 기본값 (실제 배포 시에는 제거)
        if ("dev".equals(activeProfile) || "test".equals(activeProfile)) {
            parameters.put("openai.api.key", System.getenv("OPENAI_API_KEY"));
            parameters.put("openai.timeout", "60");
            parameters.put("openai.maxRetries", "3");
            parameters.put("crop.registration.model", "gpt-4o-mini");
            parameters.put("crop.diagnosis.model", "gpt-4o");
        }
        
        return parameters;
    }

    /**
     * 파라미터 조회
     */
    public String getParameter(String key) {
        if (!parameterStoreEnabled) {
            return null;
        }
        
        String value = parameterCache.get(key);
        if (value == null) {
            // 캐시에 없으면 실시간 조회 (선택적)
            value = fetchParameterFromStore(key);
            if (value != null) {
                parameterCache.put(key, value);
            }
        }
        
        return value;
    }

    /**
     * 실시간 파라미터 조회 (캐시 미스 시)
     */
    private String fetchParameterFromStore(String key) {
        if (!parameterStoreEnabled) {
            return null;
        }
        
        try {
            // 파라미터 스토어에서 암호화된 파라미터 조회
            log.debug("실시간 파라미터 조회: {}", key);
            return null; // 실제 구현 필요
        } catch (Exception e) {
            log.warn("파라미터 조회 실패: {}", key, e);
            return null;
        }
    }

    /**
     * 파라미터 캐시 새로고침
     */
    public void refreshCache() {
        if (!parameterStoreEnabled) {
            return;
        }
        
        log.info("Parameter Store 캐시 새로고침 중...");
        parameterCache.clear();
        loadParametersFromStore();
        log.info("Parameter Store 캐시 새로고침 완료");
    }

    /**
     * OpenAI API 키 조회 (보안을 위한 전용 메서드)
     */
    public String getOpenAIApiKey() {
        String key = getParameter("openai.api.key");
        if (key == null || key.trim().isEmpty()) {
            log.warn("OpenAI API 키가 Parameter Store에서 조회되지 않았습니다.");
            // 환경변수에서 폴백 조회
            key = System.getenv("OPENAI_API_KEY");
        }
        return key;
    }

    /**
     * DB 연결 정보 조회 (필요 시)
     */
    public Map<String, String> getDatabaseCredentials() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("url", getParameter("database.url"));
        credentials.put("username", getParameter("database.username"));
        credentials.put("password", getParameter("database.password"));
        return credentials;
    }
}
