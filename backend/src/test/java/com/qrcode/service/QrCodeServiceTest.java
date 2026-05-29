package com.qrcode.service;

import com.qrcode.dto.QrCodeRequest;
import com.qrcode.dto.QrCodeResponse;
import com.qrcode.entity.QrCode;
import com.qrcode.exception.QrCodeNotFoundException;
import com.qrcode.repository.QrCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QrCodeServiceTest {

    @Mock
    private QrCodeRepository qrCodeRepository;

    @InjectMocks
    private QrCodeService qrCodeService;

    private QrCodeRequest testRequest;
    private QrCode testQrCode;

    @BeforeEach
    void setUp() {
        testRequest = QrCodeRequest.builder()
            .sublinkUrl("https://example.com/test")
            .expiresInMinutes(60)
            .createdBy("test-user")
            .build();

        testQrCode = QrCode.builder()
            .id("test-id-123")
            .sublinkUrl("https://example.com/test")
            .status("VALID")
            .createdAt(LocalDateTime.now())
            .expiredAt(LocalDateTime.now().plusMinutes(60))
            .createdBy("test-user")
            .build();
    }

    @Test
    void shouldGenerateQrCode() {
        when(qrCodeRepository.save(any(QrCode.class))).thenReturn(testQrCode);

        QrCodeResponse response = qrCodeService.generateQrCode(testRequest);

        assertNotNull(response);
        assertEquals("test-id-123", response.getId());
        assertEquals("https://example.com/test", response.getSublinkUrl());
        assertEquals("VALID", response.getStatus());
        assertEquals("test-user", response.getCreatedBy());
        verify(qrCodeRepository).save(any(QrCode.class));
    }

    @Test
    void shouldGetQrCodeById() {
        when(qrCodeRepository.findById("test-id-123")).thenReturn(Optional.of(testQrCode));

        QrCodeResponse response = qrCodeService.getQrCode("test-id-123");

        assertNotNull(response);
        assertEquals("test-id-123", response.getId());
        assertEquals("https://example.com/test", response.getSublinkUrl());
        verify(qrCodeRepository).findById("test-id-123");
    }

    @Test
    void shouldThrowExceptionWhenQrCodeNotFound() {
        when(qrCodeRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThrows(QrCodeNotFoundException.class, () -> qrCodeService.getQrCode("invalid-id"));
        verify(qrCodeRepository).findById("invalid-id");
    }

    @Test
    void shouldGetAllQrCodes() {
        List<QrCode> qrCodes = List.of(testQrCode);
        when(qrCodeRepository.findAllByOrderByCreatedAtDesc()).thenReturn(qrCodes);

        List<QrCodeResponse> responses = qrCodeService.getAllQrCodes();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("test-id-123", responses.get(0).getId());
        verify(qrCodeRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void shouldGetQrCodesByStatus() {
        List<QrCode> qrCodes = List.of(testQrCode);
        when(qrCodeRepository.findByStatusOrderByCreatedAtDesc("VALID")).thenReturn(qrCodes);

        List<QrCodeResponse> responses = qrCodeService.getQrCodesByStatus("VALID");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("VALID", responses.get(0).getStatus());
        verify(qrCodeRepository).findByStatusOrderByCreatedAtDesc("VALID");
    }

    @Test
    void shouldGenerateQrImage() {
        byte[] image = qrCodeService.generateQrImage("https://example.com/test");

        assertNotNull(image);
        assertTrue(image.length > 0);
    }

    @Test
    void shouldExpireOutdatedQrCodes() {
        QrCode expiredQrCode = testQrCode;
        List<QrCode> expiredCodes = List.of(expiredQrCode);
        when(qrCodeRepository.findExpiredQrCodes(any(LocalDateTime.class))).thenReturn(expiredCodes);

        qrCodeService.expireOutdatedQrCodes();

        assertEquals("EXPIRED", expiredQrCode.getStatus());
        verify(qrCodeRepository).saveAll(expiredCodes);
    }

    @Test
    void shouldNotExpireWhenNoOutdatedCodes() {
        when(qrCodeRepository.findExpiredQrCodes(any(LocalDateTime.class))).thenReturn(List.of());

        qrCodeService.expireOutdatedQrCodes();

        verify(qrCodeRepository, never()).saveAll(any());
    }
}
