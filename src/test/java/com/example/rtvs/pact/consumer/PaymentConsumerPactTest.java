package com.example.rtvs.pact.consumer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consumer contract test — defines what the RTVS payment consumer expects
 * from the payment provider. Running this test generates:
 *   build/pacts/rtvs-payment-consumer-rtvs-payment-provider.json
 *
 * The Authorization header is intentionally excluded from the contract;
 * the provider test's @TargetRequestFilter injects a real JWT at verification time.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "rtvs-payment-provider")
public class PaymentConsumerPactTest {

    // ── Pact interaction 1: successful payment ────────────────────────────────

    @Pact(consumer = "rtvs-payment-consumer")
    public V4Pact successfulPaymentPact(PactDslWithProvider builder) {
        return builder
                .given("U100 has sufficient funds")
                .uponReceiving("a valid payment request from U100 to U200")
                .method("POST")
                .path("/api/v1/rtp/payments")
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .stringValue("paymentRequestId", "PR-PACT-001")
                        .stringValue("senderId", "U100")
                        .stringValue("receiverId", "U200")
                        .numberType("amount", 100.00)
                        .stringValue("currency", "USD"))
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .stringType("transactionId", "TX-abc123")
                        .stringValue("status", "COMPLETED"))
                .toPact(V4Pact.class);
    }

    // ── Pact interaction 2: rejected payment (insufficient funds) ─────────────

    @Pact(consumer = "rtvs-payment-consumer")
    public V4Pact insufficientFundsPact(PactDslWithProvider builder) {
        return builder
                .given("U300 has zero balance")
                .uponReceiving("a payment request from U300 that will be rejected")
                .method("POST")
                .path("/api/v1/rtp/payments")
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .stringValue("paymentRequestId", "PR-PACT-002")
                        .stringValue("senderId", "U300")
                        .stringValue("receiverId", "U200")
                        .numberType("amount", 50.00)
                        .stringValue("currency", "USD"))
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .stringType("transactionId")
                        .stringValue("status", "REJECTED")
                        .stringValue("reasonCode", "INSUFFICIENT_FUNDS"))
                .toPact(V4Pact.class);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @PactTestFor(pactMethod = "successfulPaymentPact")
    void testSuccessfulPayment(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentRequestId", "PR-PACT-001");
        body.put("senderId", "U100");
        body.put("receiverId", "U200");
        body.put("amount", new BigDecimal("100.00"));
        body.put("currency", "USD");

        ResponseEntity<Map> response = restTemplate.exchange(
                mockServer.getUrl() + "/api/v1/rtp/payments",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("status", "COMPLETED");
        assertThat(response.getBody().get("transactionId")).isNotNull();
    }

    @Test
    @PactTestFor(pactMethod = "insufficientFundsPact")
    void testInsufficientFundsRejected(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentRequestId", "PR-PACT-002");
        body.put("senderId", "U300");
        body.put("receiverId", "U200");
        body.put("amount", new BigDecimal("50.00"));
        body.put("currency", "USD");

        ResponseEntity<Map> response = restTemplate.exchange(
                mockServer.getUrl() + "/api/v1/rtp/payments",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("status", "REJECTED");
        assertThat(response.getBody()).containsEntry("reasonCode", "INSUFFICIENT_FUNDS");
    }
}
