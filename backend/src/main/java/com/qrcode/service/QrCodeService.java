package com.qrcode.service;

import com.qrcode.dto.QrCodeRequest;
import com.qrcode.dto.QrCodeResponse;
import com.qrcode.entity.QrCode;
import com.qrcode.repository.QrCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.glxn.qrgen.javase.QRCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrCodeService {

	private final QrCodeRepository qrCodeRepository;

	public QrCodeResponse generateQrCode(QrCodeRequest request) {
		byte[] qrData = generateQrCodeData(request.getSublinkUrl());

		QrCode qrCode = QrCode.builder()
				.sublinkUrl(request.getSublinkUrl())
				.expiredAt(LocalDateTime.now().plusMinutes(request.getExpiresInMinutes()))
				.qrCodeData(qrData)
				.createdBy(request.getCreatedBy())
				.build();

		QrCode saved = qrCodeRepository.save(qrCode);
		return mapToResponse(saved);
	}

	public QrCodeResponse getQrCode(String id) {
		QrCode qrCode = qrCodeRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("QR Code not found"));
		return mapToResponse(qrCode);
	}

	public List<QrCodeResponse> getAllQrCodes() {
		return qrCodeRepository.findAllByOrderByCreatedAtDesc().stream()
				.map(this::mapToResponse)
				.collect(Collectors.toList());
	}

	public List<QrCodeResponse> getQrCodesByStatus(String status) {
		return qrCodeRepository.findByStatusOrderByCreatedAtDesc(status).stream()
				.map(this::mapToResponse)
				.collect(Collectors.toList());
	}


	public void expireOutdatedQrCodes() {
		List<QrCode> expiredCodes = qrCodeRepository.findExpiredQrCodes(LocalDateTime.now());
		expiredCodes.forEach(code -> code.setStatus("EXPIRED"));
		qrCodeRepository.saveAll(expiredCodes);
	}

	@Scheduled(fixedDelay = 60000) // runs every 60 seconds
	public void scheduleExpireOutdatedQrCodes() {
		try {
			expireOutdatedQrCodes();
		} catch (Exception e) {
			log.error("Error expiring outdated QR codes", e);
		}
	}

	private byte[] generateQrCodeData(String text) {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			QRCode.from(text)
					.withSize(250, 250)
					.writeTo(out);
			return out.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate QR code", e);
		}
	}

	private QrCodeResponse mapToResponse(QrCode qrCode) {
		return QrCodeResponse.builder()
				.id(qrCode.getId())
				.sublinkUrl(qrCode.getSublinkUrl())
				.status(qrCode.getStatus())
				.createdAt(qrCode.getCreatedAt())
				.expiredAt(qrCode.getExpiredAt())
				.createdBy(qrCode.getCreatedBy())
				.build();
	}
}
