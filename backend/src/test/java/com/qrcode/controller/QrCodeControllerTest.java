package com.qrcode.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qrcode.dto.QrCodeRequest;
import com.qrcode.dto.QrCodeResponse;
import com.qrcode.exception.QrCodeNotFoundException;
import com.qrcode.service.QrCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QrCodeController.class)
class QrCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QrCodeService qrCodeService;

    @Autowired
    private ObjectMapper objectMapper;

    private QrCodeRequest testRequest;
    private QrCodeResponse testResponse;

    @BeforeEach
    void setUp() {
        testRequest = QrCodeRequest.builder()
            .sublinkUrl("https://example.com/test")
            .expiresInMinutes(60)
            .createdBy("test-user")
            .build();

        testResponse = QrCodeResponse.builder()
            .id("test-id-123")
            .sublinkUrl("https://example.com/test")
            .status("VALID")
            .createdAt(LocalDateTime.now())
            .expiredAt(LocalDateTime.now().plusMinutes(60))
            .createdBy("test-user")
            .build();
    }

    @Test
    void shouldGenerateQrCode() throws Exception {
        when(qrCodeService.generateQrCode(any(QrCodeRequest.class))).thenReturn(testResponse);

        mockMvc.perform(post("/api/qr-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("test-id-123"))
            .andExpect(jsonPath("$.status").value("VALID"));

        verify(qrCodeService).generateQrCode(any(QrCodeRequest.class));
    }

    @Test
    void shouldReturnBadRequestForInvalidUrl() throws Exception {
        QrCodeRequest invalidRequest = QrCodeRequest.builder()
            .sublinkUrl("not-a-url")
            .expiresInMinutes(60)
            .createdBy("test-user")
            .build();

        mockMvc.perform(post("/api/qr-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForMissingUrl() throws Exception {
        QrCodeRequest invalidRequest = QrCodeRequest.builder()
            .expiresInMinutes(60)
            .createdBy("test-user")
            .build();

        mockMvc.perform(post("/api/qr-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetQrCodeById() throws Exception {
        when(qrCodeService.getQrCode("test-id-123")).thenReturn(testResponse);

        mockMvc.perform(get("/api/qr-codes/test-id-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("test-id-123"))
            .andExpect(jsonPath("$.sublinkUrl").value("https://example.com/test"));

        verify(qrCodeService).getQrCode("test-id-123");
    }

    @Test
    void shouldReturnNotFoundForInvalidId() throws Exception {
        when(qrCodeService.getQrCode("invalid-id")).thenThrow(new QrCodeNotFoundException("invalid-id"));

        mockMvc.perform(get("/api/qr-codes/invalid-id"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("QR_CODE_NOT_FOUND"));
    }

    @Test
    void shouldGetAllQrCodes() throws Exception {
        List<QrCodeResponse> responses = List.of(testResponse);
        when(qrCodeService.getAllQrCodes()).thenReturn(responses);

        mockMvc.perform(get("/api/qr-codes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("test-id-123"));

        verify(qrCodeService).getAllQrCodes();
    }

    @Test
    void shouldGetQrCodesByStatus() throws Exception {
        List<QrCodeResponse> responses = List.of(testResponse);
        when(qrCodeService.getQrCodesByStatus("VALID")).thenReturn(responses);

        mockMvc.perform(get("/api/qr-codes/status/VALID"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("VALID"));

        verify(qrCodeService).getQrCodesByStatus("VALID");
    }

    @Test
    void shouldGetQrImage() throws Exception {
        byte[] imageData = new byte[]{1, 2, 3, 4, 5};
        when(qrCodeService.getQrCode("test-id-123")).thenReturn(testResponse);
        when(qrCodeService.generateQrImage("https://example.com/test")).thenReturn(imageData);

        mockMvc.perform(get("/api/qr-codes/test-id-123/image"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_PNG));

        verify(qrCodeService).getQrCode("test-id-123");
        verify(qrCodeService).generateQrImage("https://example.com/test");
    }

    @Test
    void shouldExpireOutdatedQrCodes() throws Exception {
        mockMvc.perform(post("/api/qr-codes/expire-outdated"))
            .andExpect(status().isNoContent());

        verify(qrCodeService).expireOutdatedQrCodes();
    }
}
