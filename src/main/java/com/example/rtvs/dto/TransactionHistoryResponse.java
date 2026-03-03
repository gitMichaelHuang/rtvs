package com.example.rtvs.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TransactionHistoryResponse {
    private List<TransactionDto> transactions;
}
