package com.example.rtvs.bdd;

import com.example.rtvs.repository.RtpTransactionRepository;
import com.example.rtvs.repository.UserAccountRepository;
import io.cucumber.java.After;
import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@ScenarioScope
public class TestDataCleanup {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RtpTransactionRepository rtpTransactionRepository;

    @After
    @Transactional
    public void resetTestData() {
        userAccountRepository.findById("U100").ifPresent(u -> {
            u.setCurrentBalance(new BigDecimal("5000.00"));
            userAccountRepository.save(u);
        });
    }
}
