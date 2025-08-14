package com.planty.config;

import com.planty.common.ApiError;
import org.apache.commons.fileupload.FileUploadBase;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;

import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;


// 전체 예외 처리 클래스
@RestControllerAdvice
public class GlobalExceptionHandler {

    // DTO @Valid 바디 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldError().getDefaultMessage();
        return ApiError.of(400, "VALIDATION_ERROR", msg);
    }

    // 폼/쿼리 파라미터 바인딩 실패
    @ExceptionHandler(BindException.class)
    public ResponseEntity<?> handleBind(BindException ex) {
        String msg = ex.getBindingResult().getFieldError().getDefaultMessage();
        return ApiError.of(400, "BIND_ERROR", msg);
    }

    // 단건 파라미터(@RequestParam, @PathVariable) 제약 위반
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraint(ConstraintViolationException ex) {
        return ApiError.of(400, "CONSTRAINT_VIOLATION", ex.getMessage());
    }

    // JSON 파싱 오류
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleNotReadable(HttpMessageNotReadableException ex) {
        return ApiError.of(400, "INVALID_JSON", "요청 본문(JSON) 형식이 올바르지 않습니다.");
    }

    // 필수 파라미터 없음
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissing(MissingServletRequestParameterException ex) {
        return ApiError.of(400, "MISSING_PARAMETER", ex.getParameterName() + " 파라미터가 필요합니다.");
    }

    // HTTP 메서드 미지원
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return ApiError.of(405, "METHOD_NOT_ALLOWED", "지원하지 않는 메서드입니다.");
    }

    // 커스텀 에러 처리
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleRSE(ResponseStatusException ex) {
        String code = ex.getReason(); // "DUPLICATE_USER_ID" 등 서비스에서 실어 보낸 코드
        String msg  = switch (code) {
            case "DUPLICATE_USER_ID"   -> "이미 존재하는 아이디입니다.";
            case "DUPLICATE_NICKNAME"  -> "이미 사용 중인 닉네임입니다.";
            case "INVALID_CREDENTIALS" -> "아이디 또는 비밀번호가 올바르지 않습니다.";
            case "NOT_FOUND" -> "존재하지 않는 페이지입니다.";
            default -> "요청을 처리할 수 없습니다.";
        };
        return ApiError.of(ex.getStatusCode().value(), code != null ? code : "ERROR", msg);
    }

    // 동시에 회원가입 시 DB에서 터질 때 처리
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrity(DataIntegrityViolationException ex) {
        String text = String.valueOf(NestedExceptionUtils.getMostSpecificCause(ex).getMessage());

        if (text.contains("user_id") || text.contains("uk_users_user_id"))
            return ApiError.of(409, "DUPLICATE_USER_ID", "이미 존재하는 아이디입니다.");

        if (text.contains("nickname") || text.contains("uk_users_nickname"))
            return ApiError.of(409, "DUPLICATE_NICKNAME", "이미 사용 중인 닉네임입니다.");

        return ApiError.of(409, "CONSTRAINT_VIOLATION", "데이터 무결성 위반");
    }

    // 멀티파트: form 파트 누락 (ex. form 또는 images 파트가 아예 없을 때)
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<?> handleMissingPart(MissingServletRequestPartException ex) {
        String part = ex.getRequestPartName();
        return ApiError.of(400, "MISSING_PART", part + " 파트가 필요합니다.");
    }

    // 파일 용량 초과 (Spring 표준)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUpload(MaxUploadSizeExceededException ex) {
        return ApiError.of(413, "FILE_TOO_LARGE", "파일 용량을 확인해주세요.");
    }

    // 그 외 멀티파트 처리 중 예외(랩핑된 경우 포함)
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<?> handleMultipart(MultipartException ex) {
        Throwable root = ex.getCause();
        if (root instanceof MaxUploadSizeExceededException) {
            return ApiError.of(413, "FILE_TOO_LARGE", "파일 용량을 확인해주세요.");
        }
        // Apache Commons FileUpload를 쓸 때의 예외
        if (root instanceof FileUploadBase.FileSizeLimitExceededException ||
                root instanceof FileUploadBase.SizeLimitExceededException) {
            return ApiError.of(413, "FILE_TOO_LARGE", "파일 용량을 확인해주세요.");
        }
        return ApiError.of(400, "MULTIPART_ERROR", "파일 업로드 형식이 올바르지 않습니다.");
    }

    // 그 외 예기치 못한 에러
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleEtc(Exception ex) {
        return ApiError.of(500, "INTERNAL_ERROR", "서버 에러가 발생했습니다.");
    }
}

