package com.example.rtvs.controller;

import com.example.rtvs.dto.PaymentRequest;
import com.example.rtvs.dto.PaymentResponse;
import com.example.rtvs.dto.V2PaymentRequest;
import com.example.rtvs.enums.ProcessingMode;
import com.example.rtvs.enums.TransactionStatus;
import com.example.rtvs.security.JwtAuthEntryPoint;
import com.example.rtvs.security.JwtTokenProvider;
import com.example.rtvs.security.SecurityConfig;
import com.example.rtvs.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentControllerV2.class)
@Import({SecurityConfig.class, JwtAuthEntryPoint.class})
class PaymentControllerV2Test {

    @Autowired MockMvc mockMvc;
    @Autowired JsonMapper objectMapper;
    @MockitoBean PaymentService paymentService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(username = "U100", authorities = "ROLE_USER")
    void initiatePaymentV2_standardMode_returnsProcessingModeAndSeconds() throws Exception {
        V2PaymentRequest req = buildRequest("PR-V2-001", "U100", "U200", "150.00", null);
        PaymentResponse v1Resp = PaymentResponse.builder()
                .transactionId("TX-v2-abc")
                .status(TransactionStatus.COMPLETED.name())
                .postedAt(Instant.now())
                .build();

        when(paymentService.processPayment(any(PaymentRequest.class), eq("U100"))).thenReturn(v1Resp);

        mockMvc.perform(post("/api/v2/rtp/payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TX-v2-abc"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.processingMode").value("STANDARD"))
                .andExpect(jsonPath("$.estimatedCompletionSeconds").value(5));
    }

    @Test
    @WithMockUser(username = "U100", authorities = "ROLE_USER")
    void initiatePaymentV2_expressMode_returnsOneSecond() throws Exception {
        V2PaymentRequest req = buildRequest("PR-V2-002", "U100", "U200", "75.00", ProcessingMode.EXPRESS);
        PaymentResponse v1Resp = PaymentResponse.builder()
                .transactionId("TX-v2-express")
                .status(TransactionStatus.COMPLETED.name())
                .postedAt(Instant.now())
                .build();

        when(paymentService.processPayment(any(PaymentRequest.class), eq("U100"))).thenReturn(v1Resp);

        mockMvc.perform(post("/api/v2/rtp/payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processingMode").value("EXPRESS"))
                .andExpect(jsonPath("$.estimatedCompletionSeconds").value(1));
    }

    @Test
    @WithMockUser(username = "U100", authorities = "ROLE_USER")
    void initiatePaymentV2_rejectedPayment_noEstimatedSeconds() throws Exception {
        V2PaymentRequest req = buildRequest("PR-V2-003", "U100", "U200", "50.00", ProcessingMode.EXPRESS);
        PaymentResponse v1Resp = PaymentResponse.builder()
                .transactionId("TX-v2-rejected")
                .status(TransactionStatus.REJECTED.name())
                .reasonCode("INSUFFICIENT_FUNDS")
                .build();

        when(paymentService.processPayment(any(PaymentRequest.class), eq("U100"))).thenReturn(v1Resp);

        mockMvc.perform(post("/api/v2/rtp/payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.estimatedCompletionSeconds").doesNotExist());
    }

    @Test
    @WithMockUser(username = "U100", authorities = "ROLE_USER")
    void initiatePaymentV2_missingFields_returns400() throws Exception {
        V2PaymentRequest req = new V2PaymentRequest(); // empty — fails @Valid

        mockMvc.perform(post("/api/v2/rtp/payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void initiatePaymentV2_noToken_returns401() throws Exception {
        V2PaymentRequest req = buildRequest("PR-V2-004", "U100", "U200", "50.00", null);

        mockMvc.perform(post("/api/v2/rtp/payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "U100", authorities = "ROLE_ANALYST")
    void initiatePaymentV2_wrongRole_returns403() throws Exception {
        V2PaymentRequest req = buildRequest("PR-V2-005", "U100", "U200", "50.00", null);

        mockMvc.perform(post("/api/v2/rtp/payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    private V2PaymentRequest buildRequest(String prId, String sender, String receiver,
                                          String amount, ProcessingMode mode) {
        V2PaymentRequest r = new V2PaymentRequest();
        r.setPaymentRequestId(prId);
        r.setSenderId(sender);
        r.setReceiverId(receiver);
        r.setAmount(new BigDecimal(amount));
        r.setCurrency("USD");
        if (mode != null) {
            r.setProcessingMode(mode);
        }
        return r;
    }
}
