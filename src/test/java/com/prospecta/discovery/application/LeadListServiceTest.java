package com.prospecta.discovery.application;

import com.prospecta.discovery.infrastructure.persistence.LeadList;
import com.prospecta.discovery.infrastructure.persistence.LeadListMember;
import com.prospecta.discovery.infrastructure.persistence.LeadListMemberRepository;
import com.prospecta.discovery.infrastructure.persistence.LeadListRepository;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.repository.ProspectRepository;
import com.prospecta.shared.exception.DuplicateResourceException;
import com.prospecta.shared.exception.ProspectNotFoundException;
import com.prospecta.shared.security.TenantContext;
import com.prospecta.shared.security.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadListServiceTest {

    @Mock
    private LeadListRepository leadListRepository;

    @Mock
    private LeadListMemberRepository leadListMemberRepository;

    @Mock
    private ProspectRepository prospectRepository;

    @InjectMocks
    private LeadListService leadListService;

    private UUID orgId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        TenantContextHolder.setContext(TenantContext.of(orgId, null, "trace-test"));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Should create lead list when name is unique in organization")
    void shouldCreateLeadListSuccessfully() {
        when(leadListRepository.existsByOrganizationIdAndNameIgnoreCase(orgId, "Directeurs Sénégal"))
                .thenReturn(false);

        LeadList saved = LeadList.builder().name("Directeurs Sénégal").build();
        saved.setId(UUID.randomUUID());
        saved.setOrganizationId(orgId);

        when(leadListRepository.save(any(LeadList.class))).thenReturn(saved);

        LeadList result = leadListService.createLeadList("Directeurs Sénégal", "Description test");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Directeurs Sénégal");
        assertThat(result.getOrganizationId()).isEqualTo(orgId);
    }

    @Test
    @DisplayName("Should reject duplicate lead list name in same organization")
    void shouldRejectDuplicateListName() {
        when(leadListRepository.existsByOrganizationIdAndNameIgnoreCase(orgId, "Liste Existante"))
                .thenReturn(true);

        assertThatThrownBy(() -> leadListService.createLeadList("Liste Existante", null))
                .isInstanceOf(DuplicateResourceException.class);

        verify(leadListRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should add prospects to lead list and ignore already attached members")
    void shouldAddProspectsToList() {
        UUID listId = UUID.randomUUID();
        LeadList list = LeadList.builder().name("Liste A").build();
        list.setId(listId);
        list.setOrganizationId(orgId);

        when(leadListRepository.findByIdAndOrganizationId(listId, orgId))
                .thenReturn(Optional.of(list));

        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        Prospect prospect1 = Prospect.builder().build();
        prospect1.setId(p1);
        Prospect prospect2 = Prospect.builder().build();
        prospect2.setId(p2);

        when(prospectRepository.findByIdAndOrganizationId(p1, orgId)).thenReturn(Optional.of(prospect1));
        when(prospectRepository.findByIdAndOrganizationId(p2, orgId)).thenReturn(Optional.of(prospect2));

        when(leadListMemberRepository.existsByLeadListIdAndProspectId(listId, p1)).thenReturn(false);
        when(leadListMemberRepository.existsByLeadListIdAndProspectId(listId, p2)).thenReturn(true); // already member

        leadListService.addProspectsToList(listId, List.of(p1, p2));

        verify(leadListMemberRepository).saveAll(argThat(iterable -> {
            List<LeadListMember> members = (List<LeadListMember>) iterable;
            return members.size() == 1 && members.get(0).getProspect().getId().equals(p1);
        }));
    }

    @Test
    @DisplayName("Should retrieve prospects in list (paginated)")
    void shouldGetProspectsInList() {
        UUID listId = UUID.randomUUID();
        LeadList list = LeadList.builder().name("Liste B").build();
        list.setId(listId);
        list.setOrganizationId(orgId);

        when(leadListRepository.findByIdAndOrganizationId(listId, orgId))
                .thenReturn(Optional.of(list));

        Prospect p = Prospect.builder().firstName("Amadou").lastName("Gueye").build();
        p.setId(UUID.randomUUID());

        LeadListMember member = LeadListMember.builder().leadList(list).prospect(p).build();
        Page<LeadListMember> memberPage = new PageImpl<>(List.of(member), PageRequest.of(0, 10), 1);

        when(leadListMemberRepository.findAllByLeadListId(eq(listId), any())).thenReturn(memberPage);

        Page<Prospect> result = leadListService.getProspectsInList(listId, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFirstName()).isEqualTo("Amadou");
    }

    @Test
    @DisplayName("Should enforce tenant isolation on lead list retrieval")
    void shouldEnforceTenantIsolation() {
        UUID otherListId = UUID.randomUUID();
        when(leadListRepository.findByIdAndOrganizationId(otherListId, orgId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> leadListService.getLeadList(otherListId))
                .isInstanceOf(ProspectNotFoundException.class);
    }
}
