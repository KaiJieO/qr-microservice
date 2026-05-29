package com.qrcode.exception;

public class InvalidQrCodeRequestException extends RuntimeException {
    public InvalidQrCodeRequestException(String message) {
        super("Invalid QR code request: " + message);
    }
}
