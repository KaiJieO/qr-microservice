package com.qrcode.exception;

public class QrCodeNotFoundException extends RuntimeException {
    public QrCodeNotFoundException(String id) {
        super("QR code not found: " + id);
    }
}
