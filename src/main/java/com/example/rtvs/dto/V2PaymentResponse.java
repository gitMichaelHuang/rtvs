package com.example.rtvs.dto;

import com.example.rtvs.enums.ProcessingMode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class V2PaymentResponse {
    private String transactionId;
    private String status;
    private Instant postedAt;
    private String reasonCode;
    private String message;
    private ProcessingMode processingMode;
    private Integer estimatedCompletionSeconds;
}
