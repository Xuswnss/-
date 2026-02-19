package com.uniqdata.backend;

import com.uniqdata.backend.core.CoreClientException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e, HttpServletRequest req) {
        log.error("[ERROR] IllegalArgumentException | method={} | uri={} | query={} | message={} | exception={} | ip={} | contentType={}",
                req.getMethod(), req.getRequestURI(), req.getQueryString(),
                e.getMessage(), e.getClass().getSimpleName(), req.getRemoteAddr(), req.getContentType(), e);
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException e, HttpServletRequest req) {
        log.error("[ERROR] IllegalStateException | method={} | uri={} | query={} | message={} | exception={} | ip={} | contentType={}",
                req.getMethod(), req.getRequestURI(), req.getQueryString(),
                e.getMessage(), e.getClass().getSimpleName(), req.getRemoteAddr(), req.getContentType(), e);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(CoreClientException.class)
    public ResponseEntity<Map<String, String>> handleCoreError(CoreClientException e, HttpServletRequest req) {
        log.error("[ERROR] CoreClientException | method={} | uri={} | query={} | message={} | detail={} | ip={} | contentType={}",
                req.getMethod(), req.getRequestURI(), req.getQueryString(),
                "Core(블록체인 서버) 연동 실패", e.getMessage(), req.getRemoteAddr(), req.getContentType(), e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "Core(블록체인 서버) 연동 실패", "detail", e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleJsonParseError(HttpMessageNotReadableException e, HttpServletRequest req) {
        String rootCause = e.getCause() != null ? e.getCause().getMessage() : "null";
        String rootCauseClass = e.getCause() != null ? e.getCause().getClass().getSimpleName() : "null";
        log.error("[ERROR] HttpMessageNotReadableException | method={} | uri={} | query={} | message={} | rootCause={} | rootCauseClass={} | contentType={} | ip={}",
                req.getMethod(), req.getRequestURI(), req.getQueryString(),
                e.getMessage(), rootCause, rootCauseClass, req.getContentType(), req.getRemoteAddr(), e);
        return ResponseEntity.badRequest()
                .body(Map.of("error", "JSON 파싱 실패. Body가 올바른 JSON 형식인지 확인하세요.", "detail", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnknown(Exception e, HttpServletRequest req) {
        String userAgent = req.getHeader("User-Agent");
        log.error("[ERROR] UnhandledException | exception={} | method={} | uri={} | query={} | message={} | ip={} | contentType={} | userAgent={}",
                e.getClass().getName(), req.getMethod(), req.getRequestURI(), req.getQueryString(),
                e.getMessage(), req.getRemoteAddr(), req.getContentType(),
                userAgent != null ? userAgent.substring(0, Math.min(100, userAgent.length())) : "null", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "detail", e.getMessage()));
    }
}
