package com.example.rtvs.controller;

import com.example.rtvs.dto.PaymentRequest;
import com.example.rtvs.dto.PaymentResponse;
import com.example.rtvs.dto.V2PaymentRequest;
import com.example.rtvs.dto.V2PaymentResponse;
import com.example.rtvs.enums.ProcessingMode;
import com.example.rtvs.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v2/rtp/payments")
@RequiredArgsConstructor
@Tag(name = "Payments V2", description = "Initiate RTP payments (v2) with optional processing mode")
@SecurityRequirement(name = "JWT")
public class PaymentControllerV2 {

    private final PaymentService paymentService;

    private static final Map<ProcessingMode, Integer> COMPLETION_SECONDS =
            Map.of(ProcessingMode.STANDARD, 5, ProcessingMode.EXPRESS, 1);

    @PostMapping
    @Operation(summary = "Initiate a real-time payment (v2)")
    public ResponseEntity<V2PaymentResponse> initiatePayment(
            @Valid @RequestBody V2PaymentRequest request,
            Authentication authentication) {
        PaymentResponse v1Response = paymentService.processPayment(
                toV1Request(request), authentication.getName());
        return ResponseEntity.ok(toV2Response(v1Response, request.getProcessingMode()));
    }

    private PaymentRequest toV1Request(V2PaymentRequest req) {
        PaymentRequest v1 = new PaymentRequest();
        v1.setPaymentRequestId(req.getPaymentRequestId());
        v1.setSenderId(req.getSenderId());
        v1.setReceiverId(req.getReceiverId());
        v1.setAmount(req.getAmount());
        v1.setCurrency(req.getCurrency());
        v1.setNote(req.getNote());
        return v1;
    }

    private V2PaymentResponse toV2Response(PaymentResponse v1, ProcessingMode mode) {
        boolean completed = "COMPLETED".equals(v1.getStatus());
        return V2PaymentResponse.builder()
                .transactionId(v1.getTransactionId())
                .status(v1.getStatus())
                .postedAt(v1.getPostedAt())
                .reasonCode(v1.getReasonCode())
                .message(v1.getMessage())
                .processingMode(mode)
                .estimatedCompletionSeconds(completed ? COMPLETION_SECONDS.get(mode) : null)
                .build();
    }
}
