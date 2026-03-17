package com.example.rtvs.pact.provider;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.TargetRequestFilter;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.example.rtvs.repository.RtpTransactionRepository;
import com.example.rtvs.repository.UserAccountRepository;
import com.example.rtvs.security.JwtTokenProvider;
import org.apache.http.HttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

/**
 * Provider contract verification — replays each interaction from the pact file
 * against the running Spring Boot application and asserts that the responses match.
 *
 * Run with: ./gradlew pactProviderTest  (depends on pactConsumerTest)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Provider("rtvs-payment-provider")
@PactFolder("build/pacts")
public class PaymentProviderPactTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RtpTransactionRepository rtpTransactionRepository;

    // Tracks which user owns the current interaction so the filter can issue the right JWT
    private String currentSenderId = "U100";

    @BeforeEach
    void configureTarget(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    /**
     * Adds a real JWT to every request Pact replays against the provider.
     * The sender ID is set by the @State method before each interaction.
     */
    @TargetRequestFilter
    public void addJwtToken(HttpRequest request) {
        var auth = new UsernamePasswordAuthenticationToken(
                currentSenderId, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        String token = jwtTokenProvider.generateToken(auth);
        request.addHeader("Authorization", "Bearer " + token);
    }

    // ── Provider states ───────────────────────────────────────────────────────

    @State("U100 has sufficient funds")
    public void u100HasSufficientFunds() {
        currentSenderId = "U100";
        rtpTransactionRepository.findByPaymentRequestId("PR-PACT-001")
                .ifPresent(rtpTransactionRepository::delete);
        userAccountRepository.findById("U100").ifPresent(u -> {
            u.setCurrentBalance(new BigDecimal("5000.00"));
            userAccountRepository.save(u);
        });
        userAccountRepository.findById("U200").ifPresent(u -> {
            u.setCurrentBalance(new BigDecimal("1000.00"));
            userAccountRepository.save(u);
        });
    }

    @State("U300 has zero balance")
    public void u300HasZeroBalance() {
        currentSenderId = "U300";
        rtpTransactionRepository.findByPaymentRequestId("PR-PACT-002")
                .ifPresent(rtpTransactionRepository::delete);
        userAccountRepository.findById("U300").ifPresent(u -> {
            u.setCurrentBalance(new BigDecimal("0.00"));
            userAccountRepository.save(u);
        });
    }
}
