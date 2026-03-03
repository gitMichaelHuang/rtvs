package com.example.rtvs.service;

import com.example.rtvs.domain.LedgerEntry;
import com.example.rtvs.domain.RtpTransaction;
import com.example.rtvs.domain.UserAccount;
import com.example.rtvs.enums.EntryType;
import com.example.rtvs.repository.LedgerEntryRepository;
import com.example.rtvs.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Posts debit/credit ledger entries and updates both account balances atomically.
 * Must be called within the same @Transactional context as the payment flow so
 * that the sender lock (acquired in ValidationEngine) is still held.
 */
@Service
@RequiredArgsConstructor
public class LedgerPostingService {

    private final UserAccountRepository userAccountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public void post(RtpTransaction transaction, UserAccount lockedSender) {
        BigDecimal amount = transaction.getAmount();

        // Load receiver with pessimistic lock to prevent concurrent credit race
        UserAccount receiver = userAccountRepository.findByIdForUpdate(transaction.getReceiverId())
                .orElseThrow(() -> new IllegalStateException(
                        "Receiver not found during ledger posting: " + transaction.getReceiverId()));

        BigDecimal senderBalanceAfter = lockedSender.getCurrentBalance().subtract(amount);
        BigDecimal receiverBalanceAfter = receiver.getCurrentBalance().add(amount);

        // Update balances
        lockedSender.setCurrentBalance(senderBalanceAfter);
        userAccountRepository.save(lockedSender);

        receiver.setCurrentBalance(receiverBalanceAfter);
        userAccountRepository.save(receiver);

        // Create ledger entries
        Instant now = Instant.now();
        LedgerEntry debit = LedgerEntry.builder()
                .ledgerId(UUID.randomUUID().toString())
                .userId(transaction.getSenderId())
                .transactionId(transaction.getTransactionId())
                .entryType(EntryType.DEBIT)
                .amount(amount)
                .balanceAfter(senderBalanceAfter)
                .createdAt(now)
                .build();

        LedgerEntry credit = LedgerEntry.builder()
                .ledgerId(UUID.randomUUID().toString())
                .userId(transaction.getReceiverId())
                .transactionId(transaction.getTransactionId())
                .entryType(EntryType.CREDIT)
                .amount(amount)
                .balanceAfter(receiverBalanceAfter)
                .createdAt(now)
                .build();

        ledgerEntryRepository.saveAll(List.of(debit, credit));
    }
}
