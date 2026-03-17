package com.example.rtvs.bdd;

import com.example.rtvs.security.JwtTokenProvider;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Component
@ScenarioScope
public class PaymentStepDefinitions {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String currentToken;
    private ResponseEntity<Map> lastResponse;
    private String originalTransactionId;

    // ── Background ────────────────────────────────────────────────────────────

    @Given("the system has been seeded with standard test accounts")
    public void systemSeeded() {
        // DataInitializer runs at Spring context startup — nothing additional needed
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    @Given("user {string} is authenticated with role {string}")
    public void userAuthenticated(String userId, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority(role)));
        currentToken = jwtTokenProvider.generateToken(auth);
    }

    // ── Payment actions ───────────────────────────────────────────────────────

    @When("user {string} sends a payment of {string} USD to {string} with request id {string}")
    public void userSendsPayment(String sender, String amount, String receiver, String requestId) {
        lastResponse = restTemplate.exchange(
                "/api/v1/rtp/payments",
                HttpMethod.POST,
                buildPaymentEntity(requestId, sender, receiver, amount),
                Map.class);
    }

    @And("a payment of {string} USD to {string} with request id {string} has already been processed")
    public void paymentAlreadyProcessed(String amount, String receiver, String requestId) {
        // Make the initial payment and store the returned transactionId
        ResponseEntity<Map> firstResponse = restTemplate.exchange(
                "/api/v1/rtp/payments",
                HttpMethod.POST,
                buildPaymentEntity(requestId, "U100", receiver, amount),
                Map.class);
        assertThat(firstResponse.getStatusCode().value()).isEqualTo(200);
        originalTransactionId = (String) firstResponse.getBody().get("transactionId");
        assertThat(originalTransactionId).isNotBlank();
    }

    @When("user {string} sends a V2 payment of {string} USD to {string} with request id {string} and processingMode {string}")
    public void userSendsV2Payment(String sender, String amount, String receiver,
                                   String requestId, String processingMode) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentRequestId", requestId);
        body.put("senderId", sender);
        body.put("receiverId", receiver);
        body.put("amount", new BigDecimal(amount));
        body.put("currency", "USD");
        body.put("processingMode", processingMode);

        lastResponse = restTemplate.exchange(
                "/api/v2/rtp/payments",
                HttpMethod.POST,
                new HttpEntity<>(body, authHeaders()),
                Map.class);
    }

    // ── Assertions ────────────────────────────────────────────────────────────

    @Then("the response status is {int}")
    public void responseStatusIs(int expectedStatus) {
        assertThat(lastResponse.getStatusCode().value()).isEqualTo(expectedStatus);
    }

    @Then("the payment status is {string}")
    public void paymentStatusIs(String expectedStatus) {
        assertThat(lastResponse.getBody()).containsEntry("status", expectedStatus);
    }

    @Then("a transactionId is present in the response")
    public void transactionIdPresent() {
        assertThat(lastResponse.getBody().get("transactionId")).isNotNull();
    }

    @Then("the reason code is {string}")
    public void reasonCodeIs(String expectedCode) {
        assertThat(lastResponse.getBody()).containsEntry("reasonCode", expectedCode);
    }

    @Then("the transactionId matches the original transaction")
    public void transactionIdMatchesOriginal() {
        String returnedId = (String) lastResponse.getBody().get("transactionId");
        assertThat(returnedId).isEqualTo(originalTransactionId);
    }

    @Then("the V2 response contains processingMode {string}")
    public void v2ResponseContainsProcessingMode(String expectedMode) {
        assertThat(lastResponse.getBody()).containsEntry("processingMode", expectedMode);
    }

    @Then("the V2 response contains estimatedCompletionSeconds {int}")
    public void v2ResponseContainsEstimatedSeconds(int expectedSeconds) {
        assertThat(lastResponse.getBody().get("estimatedCompletionSeconds"))
                .isEqualTo(expectedSeconds);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private HttpEntity<Map<String, Object>> buildPaymentEntity(
            String requestId, String sender, String receiver, String amount) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentRequestId", requestId);
        body.put("senderId", sender);
        body.put("receiverId", receiver);
        body.put("amount", new BigDecimal(amount));
        body.put("currency", "USD");
        return new HttpEntity<>(body, authHeaders());
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(currentToken);
        return headers;
    }
}
