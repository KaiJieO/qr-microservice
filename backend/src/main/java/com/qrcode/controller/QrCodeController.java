package com.qrcode.controller;

import com.qrcode.dto.QrCodeRequest;
import com.qrcode.dto.QrCodeResponse;
import com.qrcode.service.QrCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/qr-codes")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class QrCodeController {

	private final QrCodeService qrCodeService;

	@PostMapping
	public ResponseEntity<QrCodeResponse> generateQrCode(@Valid @RequestBody QrCodeRequest request) {
		log.info("POST /api/qr-codes - Generating QR code for URL: {}", request.getSublinkUrl());
		QrCodeResponse response = qrCodeService.generateQrCode(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<List<QrCodeResponse>> getAllQrCodes() {
		log.debug("GET /api/qr-codes - Fetching all QR codes");
		return ResponseEntity.ok(qrCodeService.getAllQrCodes());
	}

	@GetMapping("/{id}")
	public ResponseEntity<QrCodeResponse> getQrCode(@PathVariable String id) {
		log.debug("GET /api/qr-codes/{} - Fetching QR code", id);
		return ResponseEntity.ok(qrCodeService.getQrCode(id));
	}

	@GetMapping("/{id}/image")
	public ResponseEntity<byte[]> getQrImage(@PathVariable String id) {
		log.debug("GET /api/qr-codes/{}/image - Fetching QR image", id);
		QrCodeResponse qrCode = qrCodeService.getQrCode(id);
		byte[] image = qrCodeService.generateQrImage(qrCode.getSublinkUrl());
		return ResponseEntity.ok()
				.contentType(MediaType.IMAGE_PNG)
				.body(image);
	}

	@GetMapping("/status/{status}")
	public ResponseEntity<List<QrCodeResponse>> getQrCodesByStatus(@PathVariable String status) {
		log.debug("GET /api/qr-codes/status/{} - Fetching QR codes by status", status);
		return ResponseEntity.ok(qrCodeService.getQrCodesByStatus(status));
	}

	@PostMapping("/expire-outdated")
	public ResponseEntity<Void> expireOutdatedQrCodes() {
		log.info("POST /api/qr-codes/expire-outdated - Manually expiring outdated QR codes");
		qrCodeService.expireOutdatedQrCodes();
		return ResponseEntity.noContent().build();
	}

}
