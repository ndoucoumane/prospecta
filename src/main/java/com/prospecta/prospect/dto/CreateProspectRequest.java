package com.prospecta.prospect.dto;

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
public class CreateProspectRequest {

    private UUID companyId;

    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @Size(max = 150, message = "Job title cannot exceed 150 characters")
    private String jobTitle;

    @Size(max = 255, message = "Company name cannot exceed 255 characters")
    private String companyName;

    private String companyWebsite;

    @Email(message = "Invalid email format")
    private String email;

    private String phone;
    private String whatsappNumber;

    private String country;
    private String city;
    private String region;
    private String industry;
    private String companySize;
    private String linkedinUrl;
    private String source;
}
