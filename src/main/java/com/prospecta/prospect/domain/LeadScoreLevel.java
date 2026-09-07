package com.prospecta.prospect.domain;

public enum LeadScoreLevel {
    LOW,        // 0-39
    MEDIUM,     // 40-69
    HIGH,       // 70-84
    VERY_HIGH;  // 85-100

    public static LeadScoreLevel fromScore(int score) {
        if (score >= 85) return VERY_HIGH;
        if (score >= 70) return HIGH;
        if (score >= 40) return MEDIUM;
        return LOW;
    }
}
