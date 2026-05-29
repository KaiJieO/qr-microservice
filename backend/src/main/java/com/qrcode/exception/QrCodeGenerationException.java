package com.qrcode.exception;

public class QrCodeGenerationException extends RuntimeException {
    public QrCodeGenerationException(String message, Throwable cause) {
        super("Failed to generate QR code: " + message, cause);
    }
}
