package com.qrcode.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(QrCodeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleQrCodeNotFound(QrCodeNotFoundException ex) {
        log.warn("QR code not found: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
            .errorCode("QR_CODE_NOT_FOUND")
            .message(ex.getMessage())
            .status(404)
            .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InvalidQrCodeRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidQrCodeRequestException ex) {
        log.warn("Invalid QR code request: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
            .errorCode("INVALID_REQUEST")
            .message(ex.getMessage())
            .status(400)
            .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(QrCodeGenerationException.class)
    public ResponseEntity<ErrorResponse> handleQrCodeGenerationError(QrCodeGenerationException ex) {
        log.error("QR code generation failed: {}", ex.getMessage(), ex);
        ErrorResponse error = ErrorResponse.builder()
            .errorCode("GENERATION_FAILED")
            .message("Failed to generate QR code")
            .status(500)
            .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        log.warn("Validation error: {}", errors);
        ErrorResponse error = ErrorResponse.builder()
            .errorCode("VALIDATION_ERROR")
            .message("Validation failed: " + errors)
            .status(400)
            .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralError(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        ErrorResponse error = ErrorResponse.builder()
            .errorCode("INTERNAL_ERROR")
            .message("An unexpected error occurred")
            .status(500)
            .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
