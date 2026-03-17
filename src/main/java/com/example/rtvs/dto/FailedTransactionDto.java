package com.example.rtvs.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class FailedTransactionDto {
    private String transactionId;
    private String senderId;
    private String receiverId;
    private BigDecimal amount;
    private String reasonCode;
    private Instant timestamp;
}
