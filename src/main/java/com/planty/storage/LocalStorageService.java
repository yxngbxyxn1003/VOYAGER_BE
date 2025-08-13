package com.planty.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;


// 로컬 저장 서비스
@Service
public class LocalStorageService implements StorageService {

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Override
    public String save(MultipartFile file, String folder) throws IOException {
        // 원본 파일명에서 확장자 추출 (없으면 빈 문자열)
        String ext = Optional.ofNullable(file.getOriginalFilename())
                .filter(n -> n.contains("."))
                .map(n -> n.substring(n.lastIndexOf(".")))
                .orElse("");

        // 저장 경로 생성 (중간 폴더까지)
        Path dir = Path.of(uploadDir, folder);
        Files.createDirectories(dir);

        // UUID를 이용해 중복 방지 파일명 생성
        String name = UUID.randomUUID() + ext;

        // 파일 저장 (같은 이름이 있으면 덮어씀)
        Files.copy(file.getInputStream(), dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);

        // 저장된 파일의 접근 URL 반환
        // 주의: Nginx에서 /uploads/** → uploadDir 경로 매핑 필요
        return "/uploads/" + folder + "/" + name;
    }

    @Override
    public void deleteByUrl(String url) throws IOException {
        // 업로드 URL(/uploads/...)을 실제 파일 경로로 변환 후 삭제
        Path p = Path.of(uploadDir, url.replaceFirst("^/uploads/", ""));
        Files.deleteIfExists(p);
    }

}

