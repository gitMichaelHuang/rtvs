package com.example.rtvs.service;

import com.example.rtvs.domain.RtpTransaction;
import com.example.rtvs.domain.UserAccount;
import com.example.rtvs.dto.PaymentRequest;
import com.example.rtvs.dto.PaymentResponse;
import com.example.rtvs.enums.AccountStatus;
import com.example.rtvs.enums.FailureReasonCode;
import com.example.rtvs.enums.TransactionStatus;
import com.example.rtvs.exception.RtvsException;
import com.example.rtvs.repository.RtpTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock ValidationEngine validationEngine;
    @Mock LedgerPostingService ledgerPostingService;
    @Mock RtpTransactionRepository transactionRepository;

    @InjectMocks PaymentService paymentService;

    private PaymentRequest request;
    private UserAccount lockedSender;

    @BeforeEach
    void setup() {
        request = new PaymentRequest();
        request.setPaymentRequestId("PR-001");
        request.setSenderId("U100");
        request.setReceiverId("U200");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");

        lockedSender = UserAccount.builder()
                .userId("U100")
                .currentBalance(new BigDecimal("500.00"))
                .dailyLimit(new BigDecimal("2000.00"))
                .status(AccountStatus.ACTIVE)
                .role("ROLE_USER")
                .build();
    }

    @Test
    void processPayment_success_returnsCompleted() {
        when(transactionRepository.findByPaymentRequestId("PR-001")).thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(validationEngine.validate(request)).thenReturn(lockedSender);
        doNothing().when(ledgerPostingService).post(any(), any());

        PaymentResponse response = paymentService.processPayment(request, "U100");

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED.name());
        assertThat(response.getTransactionId()).isNotBlank();
        assertThat(response.getReasonCode()).isNull();
        verify(ledgerPostingService).post(any(), eq(lockedSender));
    }

    @Test
    void processPayment_insufficientFunds_returnsRejected() {
        when(transactionRepository.findByPaymentRequestId("PR-001")).thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(validationEngine.validate(request))
                .thenThrow(new RtvsException(FailureReasonCode.INSUFFICIENT_FUNDS,
                        "Sender does not have sufficient balance"));

        PaymentResponse response = paymentService.processPayment(request, "U100");

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.REJECTED.name());
        assertThat(response.getReasonCode()).isEqualTo(FailureReasonCode.INSUFFICIENT_FUNDS.name());
        verifyNoInteractions(ledgerPostingService);
    }

    @Test
    void processPayment_idempotency_returnsOriginalResult() {
        RtpTransaction existing = RtpTransaction.builder()
                .transactionId("TX-existing")
                .paymentRequestId("PR-001")
                .status(TransactionStatus.COMPLETED)
                .senderId("U100")
                .receiverId("U200")
                .build();
        when(transactionRepository.findByPaymentRequestId("PR-001")).thenReturn(Optional.of(existing));

        PaymentResponse response = paymentService.processPayment(request, "U100");

        assertThat(response.getTransactionId()).isEqualTo("TX-existing");
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED.name());
        verifyNoInteractions(validationEngine, ledgerPostingService);
    }

    @Test
    void processPayment_ownershipViolation_throwsAccessDenied() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> paymentService.processPayment(request, "U999"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void processPayment_dailyLimitExceeded_returnsRejected() {
        when(transactionRepository.findByPaymentRequestId("PR-001")).thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(validationEngine.validate(request))
                .thenThrow(new RtvsException(FailureReasonCode.DAILY_LIMIT_EXCEEDED,
                        "Daily transaction limit exceeded"));

        PaymentResponse response = paymentService.processPayment(request, "U100");

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.REJECTED.name());
        assertThat(response.getReasonCode()).isEqualTo(FailureReasonCode.DAILY_LIMIT_EXCEEDED.name());
    }

    @Test
    void processPayment_senderNotFound_returnsRejected() {
        when(transactionRepository.findByPaymentRequestId("PR-001")).thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(validationEngine.validate(request))
                .thenThrow(new RtvsException(FailureReasonCode.SENDER_NOT_FOUND,
                        "Sender account not found"));

        PaymentResponse response = paymentService.processPayment(request, "U100");

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.REJECTED.name());
        assertThat(response.getReasonCode()).isEqualTo(FailureReasonCode.SENDER_NOT_FOUND.name());
    }
}
