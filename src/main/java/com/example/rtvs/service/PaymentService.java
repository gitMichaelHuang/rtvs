package com.example.rtvs.service;

import com.example.rtvs.domain.RtpTransaction;
import com.example.rtvs.domain.UserAccount;
import com.example.rtvs.dto.PaymentRequest;
import com.example.rtvs.dto.PaymentResponse;
import com.example.rtvs.enums.TransactionStatus;
import com.example.rtvs.exception.RtvsException;
import com.example.rtvs.repository.RtpTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final ValidationEngine validationEngine;
    private final LedgerPostingService ledgerPostingService;
    private final RtpTransactionRepository transactionRepository;

    /**
     * Full payment flow — all steps run in a single ACID transaction.
     *
     * Business-rule failures (RtvsException) are caught, the transaction is saved
     * as REJECTED, and a failure response is returned (no rollback).
     * System errors propagate and trigger a full rollback.
     */
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request, String authenticatedUserId) {

        // Ownership: a USER can only send from their own account
        if (!authenticatedUserId.equals(request.getSenderId())) {
            throw new AccessDeniedException(
                    "You can only initiate payments from your own account");
        }

        // Idempotency: if the same paymentRequestId has already been processed, return the original result
        Optional<RtpTransaction> existing =
                transactionRepository.findByPaymentRequestId(request.getPaymentRequestId());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        // Create the transaction record in INITIATED state
        RtpTransaction transaction = RtpTransaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .paymentRequestId(request.getPaymentRequestId())
                .senderId(request.getSenderId())
                .receiverId(request.getReceiverId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .note(request.getNote())
                .status(TransactionStatus.INITIATED)
                .transactionDate(LocalDate.now())
                .createdAt(Instant.now())
                .build();

        try {
            transactionRepository.save(transaction);
        } catch (DataIntegrityViolationException e) {
            // Race condition: another thread just inserted this paymentRequestId
            return transactionRepository.findByPaymentRequestId(request.getPaymentRequestId())
                    .map(this::toResponse)
                    .orElseThrow(() -> new RuntimeException("Unexpected duplicate key state", e));
        }

        try {
            // Validate — acquires pessimistic lock on sender; throws RtvsException on failure
            UserAccount lockedSender = validationEngine.validate(request);
            transaction.setStatus(TransactionStatus.VALIDATED);

            // Post debit + credit entries atomically (within the same transaction)
            ledgerPostingService.post(transaction, lockedSender);

            transaction.setStatus(TransactionStatus.COMPLETED);
            transactionRepository.save(transaction);

            return PaymentResponse.builder()
                    .transactionId(transaction.getTransactionId())
                    .status(TransactionStatus.COMPLETED.name())
                    .postedAt(transaction.getCreatedAt())
                    .build();

        } catch (RtvsException e) {
            // Business rule violation — persist the rejection; do NOT rethrow (no rollback)
            transaction.setStatus(TransactionStatus.REJECTED);
            transaction.setFailureReason(e.getReasonCode());
            transactionRepository.save(transaction);

            return PaymentResponse.builder()
                    .transactionId(transaction.getTransactionId())
                    .status(TransactionStatus.REJECTED.name())
                    .reasonCode(e.getReasonCode().name())
                    .message(e.getMessage())
                    .build();
        }
    }

    private PaymentResponse toResponse(RtpTransaction tx) {
        return PaymentResponse.builder()
                .transactionId(tx.getTransactionId())
                .status(tx.getStatus().name())
                .postedAt(tx.getCreatedAt())
                .reasonCode(tx.getFailureReason() != null ? tx.getFailureReason().name() : null)
                .message(tx.getFailureReason() != null ? tx.getFailureReason().name().replace('_', ' ') : null)
                .build();
    }
}
