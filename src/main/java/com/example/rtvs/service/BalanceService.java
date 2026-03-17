package com.example.rtvs.service;

import com.example.rtvs.domain.UserAccount;
import com.example.rtvs.dto.BalanceResponse;
import com.example.rtvs.exception.ResourceNotFoundException;
import com.example.rtvs.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final UserAccountRepository userAccountRepository;

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String userId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        return BalanceResponse.builder()
                .userId(account.getUserId())
                .currentBalance(account.getCurrentBalance())
                .dailyLimit(account.getDailyLimit())
                .build();
    }
}
