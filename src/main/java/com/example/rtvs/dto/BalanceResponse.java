package com.example.rtvs.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BalanceResponse {
    private String userId;
    private BigDecimal currentBalance;
    private BigDecimal dailyLimit;
}
