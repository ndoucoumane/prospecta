package com.prospecta.discovery.infrastructure.persistence;

import com.prospecta.shared.domain.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "lead_lists")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadList extends TenantAwareEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "leadList", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<LeadListMember> members = new HashSet<>();
}
