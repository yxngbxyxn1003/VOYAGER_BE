package com.planty.storage;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;


// 이미지 저장 및 삭제 서비스 (상대경로 사용)
public interface StorageService {
    String save(MultipartFile file, String folder) throws IOException;

    void deleteByUrl(String url) throws IOException;
}

