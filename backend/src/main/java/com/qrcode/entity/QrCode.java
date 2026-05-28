package com.qrcode.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "qr_codes")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrCode {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(columnDefinition = "CHAR(36)")
	private String id;

	@Column(nullable = false, length = 500)
	private String sublinkUrl;

	@Column(nullable = false, length = 20)
	@Builder.Default
	private String status = "VALID"; // VALID, EXPIRED

	@Column(nullable = false)
	@Builder.Default
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(nullable = false)
	private LocalDateTime expiredAt;

	@Column(columnDefinition = "LONGBLOB")
	private byte[] qrCodeData;

	@Column(length = 100)
	private String createdBy;

	@PrePersist
	protected void onCreate() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}
}
