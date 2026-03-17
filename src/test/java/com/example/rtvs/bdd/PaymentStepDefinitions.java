package com.example.rtvs.bdd;

import com.example.rtvs.repository.UserAccountRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PaymentStepDefinitions {


    // reusable type for map responses for RestTemplate to deserialize
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    @LocalServerPort
    private int port;

    @Autowired
    private UserAccountRepository userAccountRepository;


    // http client used to call endpoints and parse responses
    private final RestTemplate restTemplate = new RestTemplate();

    private String jwtToken;

    // keep most recent for later gherkin steps
    private ResponseEntity<Map<String, Object>> lastResponse;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    // STEP ONE
    // verifies U100 and U200 exists from background step

    @Given("the system has been seeded with standard test accounts")
    public void theSystemHasBeenSeededWithStandardTestAccounts() {
        assertThat(userAccountRepository.findById("U100")).isPresent();
        assertThat(userAccountRepository.findById("U200")).isPresent();
    }

    // STEP ONE
    // calls POST /auth/login to get the JWT token
    // builds json headers and login body of userId,password
    @Given("user {string} is authenticated with role {string}")
    public void userIsAuthenticatedWithRole(String userId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> loginBody = Map.of("userId", userId, "password", "password");

        ResponseEntity<Map<String, Object>> loginResponse = restTemplate.exchange(
                baseUrl() + "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginBody, headers),
                MAP_TYPE
        );

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        jwtToken = (String) loginResponse.getBody().get("token");
        assertThat(jwtToken).isNotBlank();
    }


    // STEP TWO
    // calls POST api/v1/rtp/payments with the JWT token
    // builds json headers and sets auth bearer
    @When("user {string} sends a payment of {string} USD to {string} with request id {string}")
    public void userSendsPayment(String senderId, String amount, String receiverId, String requestId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken);

        // builds json body
        Map<String, Object> paymentBody = Map.of(
                "paymentRequestId", requestId,
                "senderId", senderId,
                "receiverId", receiverId,
                "amount", new BigDecimal(amount),
                "currency", "USD"
        );


        // stores for cache
        lastResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/rtp/payments",
                HttpMethod.POST,
                new HttpEntity<>(paymentBody, headers),
                MAP_TYPE
        );
    }


    // get status code
    @Then("the response status is {int}")
    public void theResponseStatusIs(int expectedStatus) {
        assertThat(lastResponse.getStatusCode().value()).isEqualTo(expectedStatus);
    }

    // get status code
    @And("the payment status is {string}")
    public void thePaymentStatusIs(String expectedStatus) {
        assertThat(lastResponse.getBody()).containsEntry("status", expectedStatus);
    }


    // get transactionId see if it's there
    @And("a transactionId is present in the response")
    public void aTransactionIdIsPresentInTheResponse() {
        assertThat(lastResponse.getBody()).containsKey("transactionId");
        assertThat((String) lastResponse.getBody().get("transactionId")).isNotBlank();
    }
}

// data exist -> auth jwt -> send payment request -> assert HTTP status