package com.qrcode.controller;

import com.qrcode.dto.QrCodeRequest;
import com.qrcode.dto.QrCodeResponse;
import com.qrcode.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/qr-codes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QrCodeController {

	private final QrCodeService qrCodeService;

	@PostMapping
	public ResponseEntity<QrCodeResponse> generateQrCode(@RequestBody QrCodeRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(qrCodeService.generateQrCode(request));
	}

	@GetMapping
	public ResponseEntity<List<QrCodeResponse>> getAllQrCodes() {
		return ResponseEntity.ok(qrCodeService.getAllQrCodes());
	}

	@GetMapping("/{id}")
	public ResponseEntity<QrCodeResponse> getQrCode(@PathVariable String id) {
		return ResponseEntity.ok(qrCodeService.getQrCode(id));
	}

	@GetMapping("/status/{status}")
	public ResponseEntity<List<QrCodeResponse>> getQrCodesByStatus(@PathVariable String status) {
		return ResponseEntity.ok(qrCodeService.getQrCodesByStatus(status));
	}

	@PostMapping("/expire-outdated")
	public ResponseEntity<Void> expireOutdatedQrCodes() {
		qrCodeService.expireOutdatedQrCodes();
		return ResponseEntity.noContent().build();
	}

}
