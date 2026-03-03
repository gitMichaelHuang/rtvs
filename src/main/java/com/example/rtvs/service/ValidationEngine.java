package com.example.rtvs.service;

import com.example.rtvs.domain.UserAccount;
import com.example.rtvs.dto.PaymentRequest;
import com.example.rtvs.enums.AccountStatus;
import com.example.rtvs.enums.FailureReasonCode;
import com.example.rtvs.enums.TransactionStatus;
import com.example.rtvs.exception.RtvsException;
import com.example.rtvs.repository.RtpTransactionRepository;
import com.example.rtvs.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Validates all business rules for a payment request.
 * Must be called within an active @Transactional context so that
 * the pessimistic lock on the sender is held until ledger posting completes.
 */
@Service
@RequiredArgsConstructor
public class ValidationEngine {

    private final UserAccountRepository userAccountRepository;
    private final RtpTransactionRepository transactionRepository;

    /**
     * Runs all validation checks in order.
     * Throws {@link RtvsException} on the first failure.
     * Returns the pessimistically-locked sender account for use in ledger posting.
     */
    public UserAccount validate(PaymentRequest request) {

        // 1. Amount must be positive (belt-and-suspenders — @Valid catches this too)
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RtvsException(FailureReasonCode.INVALID_AMOUNT, "Amount must be greater than 0");
        }

        // 2. Sender must exist and be ACTIVE
        UserAccount sender = userAccountRepository.findById(request.getSenderId())
                .orElseThrow(() -> new RtvsException(
                        FailureReasonCode.SENDER_NOT_FOUND,
                        "Sender account not found: " + request.getSenderId()));
        if (sender.getStatus() == AccountStatus.BLOCKED) {
            throw new RtvsException(FailureReasonCode.ACCOUNT_BLOCKED, "Sender account is blocked");
        }

        // 3. Receiver must exist and be ACTIVE
        UserAccount receiver = userAccountRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new RtvsException(
                        FailureReasonCode.RECEIVER_NOT_FOUND,
                        "Receiver account not found: " + request.getReceiverId()));
        if (receiver.getStatus() == AccountStatus.BLOCKED) {
            throw new RtvsException(FailureReasonCode.ACCOUNT_BLOCKED, "Receiver account is blocked");
        }

        // 4. Acquire pessimistic write lock on sender to guard balance and daily-limit checks
        UserAccount lockedSender = userAccountRepository.findByIdForUpdate(request.getSenderId())
                .orElseThrow(() -> new RtvsException(
                        FailureReasonCode.SENDER_NOT_FOUND, "Sender account not found"));

        // 5. Sender must have sufficient balance
        if (lockedSender.getCurrentBalance().compareTo(request.getAmount()) < 0) {
            throw new RtvsException(FailureReasonCode.INSUFFICIENT_FUNDS,
                    "Sender does not have sufficient balance");
        }

        // 6. Daily limit must not be exceeded
        BigDecimal dailyTotal = transactionRepository.sumDailyAmount(
                request.getSenderId(), LocalDate.now(), TransactionStatus.COMPLETED);
        if (dailyTotal.add(request.getAmount()).compareTo(lockedSender.getDailyLimit()) > 0) {
            throw new RtvsException(FailureReasonCode.DAILY_LIMIT_EXCEEDED,
                    "Daily transaction limit exceeded");
        }

        return lockedSender;
    }
}
