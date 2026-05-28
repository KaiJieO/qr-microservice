package com.qrcode.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrCodeRequest {

	private String sublinkUrl;

	@Builder.Default
	private int expiresInMinutes = 60;

	private String createdBy;

}
