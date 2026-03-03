package com.example.rtvs.config;

import com.example.rtvs.domain.UserAccount;
import com.example.rtvs.enums.AccountStatus;
import com.example.rtvs.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedSampleData() {
        return args -> {
            if (userAccountRepository.count() > 0) {
                log.info("Sample data already present — skipping seed");
                return;
            }

            String pw = passwordEncoder.encode("password");

            userAccountRepository.saveAll(List.of(
                    user("U100", "5000.00", "2000.00", AccountStatus.ACTIVE, pw, "ROLE_USER"),
                    user("U200", "1000.00", "1000.00", AccountStatus.ACTIVE, pw, "ROLE_USER"),
                    user("U300",    "0.00",  "500.00", AccountStatus.ACTIVE, pw, "ROLE_USER"),
                    user("U_BLOCKED", "100.00", "500.00", AccountStatus.BLOCKED, pw, "ROLE_USER"),
                    user("ANALYST1",  "0.00",   "0.00", AccountStatus.ACTIVE, pw, "ROLE_ANALYST"),
                    user("ADMIN1",    "0.00",   "0.00", AccountStatus.ACTIVE, pw, "ROLE_ADMIN")
            ));

            log.info("Sample data seeded: U100, U200, U300, U_BLOCKED, ANALYST1, ADMIN1 (password: 'password')");
        };
    }

    private UserAccount user(String id, String balance, String limit,
                             AccountStatus status, String pw, String role) {
        return UserAccount.builder()
                .userId(id)
                .currentBalance(new BigDecimal(balance))
                .dailyLimit(new BigDecimal(limit))
                .status(status)
                .password(pw)
                .role(role)
                .build();
    }
}
