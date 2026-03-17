package com.example.rtvs.controller;

import com.example.rtvs.dto.PaymentRequest;
import com.example.rtvs.dto.PaymentResponse;
import com.example.rtvs.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rtp/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Initiate RTP payments")
@SecurityRequirement(name = "JWT")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Initiate a real-time payment")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                paymentService.processPayment(request, authentication.getName()));
    }
}
