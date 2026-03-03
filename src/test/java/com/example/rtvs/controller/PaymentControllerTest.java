package com.example.rtvs.controller;

import com.example.rtvs.dto.PaymentRequest;
import com.example.rtvs.dto.PaymentResponse;
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

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, JwtAuthEntryPoint.class})
class PaymentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JsonMapper objectMapper;
    @MockitoBean PaymentService paymentService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(username = "U100", authorities = "ROLE_USER")
    void initiatePayment_validRequest_returns200() throws Exception {
        PaymentRequest req = buildRequest("PR-001", "U100", "U200", "150.00");
        PaymentResponse resp = PaymentResponse.builder()
                .transactionId("TX-abc123")
                .status(TransactionStatus.COMPLETED.name())
                .postedAt(Instant.now())
                .build();

        when(paymentService.processPayment(any(PaymentRequest.class), eq("U100"))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/rtp/payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TX-abc123"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(username = "U100", authorities = "ROLE_USER")
    void initiatePayment_missingFields_returns400() throws Exception {
        PaymentRequest req = new PaymentRequest(); // empty — will fail @Valid

        mockMvc.perform(post("/api/v1/rtp/payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void initiatePayment_noToken_returns401() throws Exception {
        PaymentRequest req = buildRequest("PR-002", "U100", "U200", "50.00");

        mockMvc.perform(post("/api/v1/rtp/payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "U100", authorities = "ROLE_ANALYST")
    void initiatePayment_wrongRole_returns403() throws Exception {
        PaymentRequest req = buildRequest("PR-003", "U100", "U200", "50.00");

        mockMvc.perform(post("/api/v1/rtp/payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    private PaymentRequest buildRequest(String prId, String sender, String receiver, String amount) {
        PaymentRequest r = new PaymentRequest();
        r.setPaymentRequestId(prId);
        r.setSenderId(sender);
        r.setReceiverId(receiver);
        r.setAmount(new BigDecimal(amount));
        r.setCurrency("USD");
        return r;
    }
}