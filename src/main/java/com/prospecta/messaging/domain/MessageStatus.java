package com.prospecta.messaging.domain;

public enum MessageStatus {
    PENDING,
    QUEUED,
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}
