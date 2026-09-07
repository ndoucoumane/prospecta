package com.prospecta.ai.service;

import com.prospecta.ai.domain.AiUsage;
import com.prospecta.ai.dto.AiDto.AiUsageSummary;
import com.prospecta.ai.repository.AiUsageRepository;
import com.prospecta.organization.domain.Organization;
import com.prospecta.organization.domain.OrganizationPlan;
import com.prospecta.organization.repository.OrganizationRepository;
import com.prospecta.shared.exception.OrganizationNotFoundException;
import com.prospecta.shared.exception.QuotaExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final AiUsageRepository aiUsageRepository;
    private final OrganizationRepository organizationRepository;

    public void checkQuota(UUID organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        long limit = getPlanQuota(org.getPlan());
        Instant startOfMonth = YearMonth.now().atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        long currentCount = aiUsageRepository.countMonthlyOperations(organizationId, startOfMonth);

        if (currentCount >= limit) {
            log.warn("Quota exceeded for organization {}: current={}, limit={}", organizationId, currentCount, limit);
            throw new QuotaExceededException(String.format(
                    "AI operations monthly quota exceeded for your %s plan (%d / %d). Please upgrade your plan.",
                    org.getPlan().name(), currentCount, limit
            ));
        }
    }

    @Transactional
    public void recordUsage(
            UUID organizationId,
            UUID userId,
            String provider,
            String model,
            String operation,
            int inputTokens,
            int outputTokens,
            long durationMs
    ) {
        // Approximate GPT-4o-mini price: $0.15 / 1M input, $0.60 / 1M output
        BigDecimal inputCost = BigDecimal.valueOf(inputTokens)
                .multiply(BigDecimal.valueOf(0.00000015));
        BigDecimal outputCost = BigDecimal.valueOf(outputTokens)
                .multiply(BigDecimal.valueOf(0.00000060));
        BigDecimal totalCost = inputCost.add(outputCost).setScale(6, RoundingMode.HALF_UP);

        AiUsage usage = AiUsage.builder()
                .organizationId(organizationId)
                .userId(userId)
                .provider(provider)
                .model(model)
                .operation(operation)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .estimatedCostUsd(totalCost)
                .durationMs(durationMs)
                .build();

        aiUsageRepository.save(usage);
        log.debug("Recorded AI usage: org={}, op={}, tokens={}+{}, cost=${}",
                organizationId, operation, inputTokens, outputTokens, totalCost);
    }

    @Transactional(readOnly = true)
    public AiUsageSummary getUsageSummary(UUID organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        long limit = getPlanQuota(org.getPlan());
        Instant startOfMonth = YearMonth.now().atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        long operations = aiUsageRepository.countMonthlyOperations(organizationId, startOfMonth);
        long tokens = aiUsageRepository.sumMonthlyTokens(organizationId, startOfMonth);

        return AiUsageSummary.builder()
                .monthlyOperations(operations)
                .monthlyTokens(tokens)
                .quotaLimit(limit)
                .plan(org.getPlan().name())
                .build();
    }

    public static long getPlanQuota(OrganizationPlan plan) {
        return switch (plan) {
            case FREE -> 100L;
            case STARTER -> 1000L;
            case BUSINESS -> 5000L;
        };
    }
}
