package com.prospecta.ai.service;

import com.prospecta.ai.repository.AiUsageRepository;
import com.prospecta.organization.domain.Organization;
import com.prospecta.organization.domain.OrganizationPlan;
import com.prospecta.organization.repository.OrganizationRepository;
import com.prospecta.shared.exception.QuotaExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuotaServiceTest {

    @Mock
    private AiUsageRepository aiUsageRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private QuotaService quotaService;

    @Test
    @DisplayName("Should pass quota check when operations count is below plan limit")
    void shouldPassWhenUnderQuota() {
        UUID orgId = UUID.randomUUID();
        Organization org = Organization.builder().plan(OrganizationPlan.FREE).build();

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(aiUsageRepository.countMonthlyOperations(eq(orgId), any())).thenReturn(45L);

        assertThatNoException().isThrownBy(() -> quotaService.checkQuota(orgId));
    }

    @Test
    @DisplayName("Should throw QuotaExceededException when operations count reaches plan limit")
    void shouldThrowWhenQuotaExceeded() {
        UUID orgId = UUID.randomUUID();
        Organization org = Organization.builder().plan(OrganizationPlan.FREE).build(); // Free limit: 100

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(aiUsageRepository.countMonthlyOperations(eq(orgId), any())).thenReturn(100L);

        assertThatThrownBy(() -> quotaService.checkQuota(orgId))
                .isInstanceOf(QuotaExceededException.class)
                .hasMessageContaining("AI operations monthly quota exceeded for your FREE plan (100 / 100)");
    }
}
