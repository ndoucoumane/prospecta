package com.prospecta.prospect.dto;

import com.prospecta.prospect.domain.ProspectStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProspectRequest {

    private UUID companyId;

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 150)
    private String jobTitle;

    @Email
    private String email;

    private String phone;
    private String whatsappNumber;

    private String city;
    private String region;
    private String industry;
    private String linkedinUrl;
    private ProspectStatus status;
}
