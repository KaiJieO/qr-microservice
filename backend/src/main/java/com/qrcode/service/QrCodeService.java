package com.qrcode.service;

import com.qrcode.dto.QrCodeRequest;
import com.qrcode.dto.QrCodeResponse;
import com.qrcode.entity.QrCode;
import com.qrcode.exception.QrCodeGenerationException;
import com.qrcode.exception.QrCodeNotFoundException;
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
		log.info("Generating QR code for URL: {} by {}", request.getSublinkUrl(), request.getCreatedBy());

		QrCode qrCode = QrCode.builder()
				.sublinkUrl(request.getSublinkUrl())
				.expiredAt(LocalDateTime.now().plusMinutes(request.getExpiresInMinutes()))
				.createdBy(request.getCreatedBy())
				.build();

		QrCode saved = qrCodeRepository.save(qrCode);
		log.info("QR code generated successfully: {} for URL: {}", saved.getId(), request.getSublinkUrl());
		return mapToResponse(saved);
	}

	public QrCodeResponse getQrCode(String id) {
		log.debug("Fetching QR code: {}", id);
		QrCode qrCode = qrCodeRepository.findById(id)
				.orElseThrow(() -> new QrCodeNotFoundException(id));
		return mapToResponse(qrCode);
	}

	public List<QrCodeResponse> getAllQrCodes() {
		log.debug("Fetching all QR codes");
		return qrCodeRepository.findAllByOrderByCreatedAtDesc().stream()
				.map(this::mapToResponse)
				.collect(Collectors.toList());
	}

	public List<QrCodeResponse> getQrCodesByStatus(String status) {
		log.debug("Fetching QR codes with status: {}", status);
		return qrCodeRepository.findByStatusOrderByCreatedAtDesc(status).stream()
				.map(this::mapToResponse)
				.collect(Collectors.toList());
	}

	public byte[] generateQrImage(String sublinkUrl) {
		log.debug("Generating QR image for URL: {}", sublinkUrl);
		return generateQrCodeData(sublinkUrl);
	}

	public void expireOutdatedQrCodes() {
		log.debug("Expiring outdated QR codes");
		List<QrCode> expiredCodes = qrCodeRepository.findExpiredQrCodes(LocalDateTime.now());
		if (expiredCodes.isEmpty()) {
			log.debug("No outdated QR codes found");
			return;
		}
		expiredCodes.forEach(code -> code.setStatus("EXPIRED"));
		qrCodeRepository.saveAll(expiredCodes);
		log.info("Expired {} outdated QR codes", expiredCodes.size());
	}

	@Scheduled(fixedDelay = 60000)
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
			log.error("Failed to generate QR code for URL: {}", text, e);
			throw new QrCodeGenerationException("QR generation error", e);
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
