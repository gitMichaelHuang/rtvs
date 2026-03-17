package com.example.rtvs.repository;

import com.example.rtvs.domain.RtpTransaction;
import com.example.rtvs.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RtpTransactionRepository extends JpaRepository<RtpTransaction, String> {

    Optional<RtpTransaction> findByPaymentRequestId(String paymentRequestId);

    @Query("SELECT t FROM RtpTransaction t " +
           "WHERE (t.senderId = :userId OR t.receiverId = :userId) " +
           "AND t.transactionDate BETWEEN :from AND :to " +
           "ORDER BY t.createdAt DESC")
    List<RtpTransaction> findByUserIdAndDateRange(@Param("userId") String userId,
                                                  @Param("from") LocalDate from,
                                                  @Param("to") LocalDate to);

    List<RtpTransaction> findByStatusAndTransactionDateBetweenOrderByCreatedAtDesc(
            TransactionStatus status, LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM RtpTransaction t " +
           "WHERE t.senderId = :senderId " +
           "AND t.transactionDate = :date " +
           "AND t.status = :status")
    BigDecimal sumDailyAmount(@Param("senderId") String senderId,
                              @Param("date") LocalDate date,
                              @Param("status") TransactionStatus status);
}
