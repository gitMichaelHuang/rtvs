package com.example.rtvs.service;

import com.example.rtvs.domain.RtpTransaction;
import com.example.rtvs.dto.FailedTransactionDto;
import com.example.rtvs.dto.FailedTransactionsResponse;
import com.example.rtvs.dto.TransactionDto;
import com.example.rtvs.dto.TransactionHistoryResponse;
import com.example.rtvs.enums.TransactionStatus;
import com.example.rtvs.repository.RtpTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final RtpTransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public TransactionHistoryResponse getTransactionHistory(String userId,
                                                            LocalDate from,
                                                            LocalDate to) {
        List<TransactionDto> dtos = transactionRepository
                .findByUserIdAndDateRange(userId, from, to)
                .stream()
                .map(tx -> TransactionDto.builder()
                        .transactionId(tx.getTransactionId())
                        .amount(tx.getAmount())
                        .status(tx.getStatus().name())
                        .direction(tx.getSenderId().equals(userId) ? "DEBIT" : "CREDIT")
                        .senderId(tx.getSenderId())
                        .receiverId(tx.getReceiverId())
                        .currency(tx.getCurrency())
                        .note(tx.getNote())
                        .reasonCode(tx.getFailureReason() != null ? tx.getFailureReason().name() : null)
                        .timestamp(tx.getCreatedAt())
                        .build())
                .toList();

        return TransactionHistoryResponse.builder().transactions(dtos).build();
    }

    @Transactional(readOnly = true)
    public FailedTransactionsResponse getFailedTransactions(LocalDate from, LocalDate to) {
        List<FailedTransactionDto> dtos = transactionRepository
                .findByStatusAndTransactionDateBetweenOrderByCreatedAtDesc(
                        TransactionStatus.REJECTED, from, to)
                .stream()
                .map(tx -> FailedTransactionDto.builder()
                        .transactionId(tx.getTransactionId())
                        .senderId(tx.getSenderId())
                        .receiverId(tx.getReceiverId())
                        .amount(tx.getAmount())
                        .reasonCode(tx.getFailureReason() != null ? tx.getFailureReason().name() : null)
                        .timestamp(tx.getCreatedAt())
                        .build())
                .toList();

        return FailedTransactionsResponse.builder().failedTransactions(dtos).build();
    }
}
