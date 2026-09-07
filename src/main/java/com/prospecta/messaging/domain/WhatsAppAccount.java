package com.prospecta.messaging.domain;

import com.prospecta.shared.domain.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "whatsapp_accounts")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppAccount extends TenantAwareEntity {

    @Column(name = "business_account_id", nullable = false, length = 100)
    private String businessAccountId;

    @Column(name = "phone_number_id", nullable = false, length = 100)
    private String phoneNumberId;

    @Column(name = "display_phone_number", length = 50)
    private String displayPhoneNumber;

    @Column(name = "encrypted_access_token", nullable = false, columnDefinition = "TEXT")
    private String encryptedAccessToken;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "ACTIVE";
}
