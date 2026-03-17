package com.example.rtvs.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FailedTransactionsResponse {
    private List<FailedTransactionDto> failedTransactions;
}
