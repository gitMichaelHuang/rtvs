package com.example.rtvs.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionDto {
    private String transactionId;
    private BigDecimal amount;
    private String status;
    private String direction;
    private String senderId;
    private String receiverId;
    private String currency;
    private String note;
    private String reasonCode;
    private Instant timestamp;
}
