package com.example.rtvs.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {
    private String transactionId;
    private String status;
    private Instant postedAt;
    private String reasonCode;
    private String message;
}
