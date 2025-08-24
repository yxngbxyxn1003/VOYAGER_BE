package com.planty.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


// 프론트 전달용 이미지 경로
@Component
public class ImageUrlMapper {
    @Value("${upload-dir}")           // 예: /srv/app/app/uploads
    private String uploadDir;

    @Value("${image.base-url:}")      // 선택: http://도메인  (비우면 상대경로 유지)
    private String baseUrl;

    public String toPublic(String stored) {
        if (stored == null || stored.isBlank()) return stored;
        String s = stored.replace('\\', '/');

        // 이미 /uploads 로 시작하면 그대로(필요 시 baseUrl 붙이기)
        if (s.startsWith("/uploads/")) {
            return baseUrl.isBlank() ? s : baseUrl + s;
        }

        // 풀 경로(/srv/app/app/uploads/...)면 /uploads/... 로 치환
        String root = uploadDir.replace('\\', '/');
        if (s.startsWith(root)) {
            String rel = s.substring(root.length());
            if (!rel.startsWith("/")) rel = "/" + rel;
            String pub = "/uploads" + rel;      // → /uploads/crop/uuid.jpg
            return baseUrl.isBlank() ? pub : baseUrl + pub;
        }

        // 그 밖의 경우는 그대로 반환(로직에 맞게 조정 가능)
        return s;
    }
}

