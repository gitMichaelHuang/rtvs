package com.example.rtvs.domain;

import com.example.rtvs.enums.FailureReasonCode;
import com.example.rtvs.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "rtp_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RtpTransaction {

    @Id
    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "payment_request_id", unique = true, nullable = false)
    private String paymentRequestId;

    @Column(name = "sender_id", nullable = false)
    private String senderId;

    @Column(name = "receiver_id", nullable = false)
    private String receiverId;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    private String currency;

    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason")
    private FailureReasonCode failureReason;

    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
