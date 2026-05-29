package com.qrcode.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrCodeRequest {

	@NotBlank(message = "URL is required")
	@URL(message = "URL must be valid (http/https)")
	@Length(max = 500, message = "URL cannot exceed 500 characters")
	private String sublinkUrl;

	@Min(value = 1, message = "Expiry must be at least 1 minute")
	@Max(value = 525600, message = "Expiry cannot exceed 1 year (525600 minutes)")
	@Builder.Default
	private int expiresInMinutes = 60;

	@NotBlank(message = "Creator name is required")
	@Length(max = 100, message = "Creator name cannot exceed 100 characters")
	private String createdBy;

}
