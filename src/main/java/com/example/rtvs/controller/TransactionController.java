package com.example.rtvs.controller;

import com.example.rtvs.dto.FailedTransactionsResponse;
import com.example.rtvs.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/rtp/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Failed transaction audit log")
@SecurityRequirement(name = "JWT")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/failed")
    @Operation(summary = "Get all failed transactions within a date range (ANALYST/ADMIN only)")
    public ResponseEntity<FailedTransactionsResponse> getFailedTransactions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(transactionService.getFailedTransactions(from, to));
    }
}
