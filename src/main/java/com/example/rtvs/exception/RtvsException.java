package com.example.rtvs.exception;

import com.example.rtvs.enums.FailureReasonCode;
import lombok.Getter;

@Getter
public class RtvsException extends RuntimeException {

    private final FailureReasonCode reasonCode;

    public RtvsException(FailureReasonCode reasonCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
    }
}
