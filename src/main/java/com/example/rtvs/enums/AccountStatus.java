package com.example.rtvs.enums;

public enum AccountStatus {
    ACTIVE, BLOCKED;

    public boolean isBlocked() {
        return this == BLOCKED;
    }
}
