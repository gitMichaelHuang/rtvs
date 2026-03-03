package com.example.rtvs.controller;

import com.example.rtvs.dto.BalanceResponse;
import com.example.rtvs.dto.TransactionHistoryResponse;
import com.example.rtvs.service.BalanceService;
import com.example.rtvs.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/rtp/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Account balance and transaction history")
@SecurityRequirement(name = "JWT")
public class UserController {

    private final BalanceService balanceService;
    private final TransactionService transactionService;

    @GetMapping("/{userId}/balance")
    @PreAuthorize("authentication.name == #userId")
    @Operation(summary = "Get current balance for a user")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String userId) {
        return ResponseEntity.ok(balanceService.getBalance(userId));
    }

    @GetMapping("/{userId}/transactions")
    @PreAuthorize("authentication.name == #userId")
    @Operation(summary = "Get transaction history for a user within a date range")
    public ResponseEntity<TransactionHistoryResponse> getTransactions(
            @PathVariable String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(transactionService.getTransactionHistory(userId, from, to));
    }
}
