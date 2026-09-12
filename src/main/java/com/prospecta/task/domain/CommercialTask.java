package com.prospecta.task.domain;

import com.prospecta.shared.domain.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "commercial_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommercialTask extends TenantAwareEntity {

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "prospect_id")
    private UUID prospectId;

    @Column(name = "prospect_name")
    private String prospectName;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "phone", length = 100)
    private String phone;

    @Column(name = "due_date", nullable = false, length = 50)
    private String dueDate;

    @Column(name = "due_time", length = 20)
    private String dueTime;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "pending";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
