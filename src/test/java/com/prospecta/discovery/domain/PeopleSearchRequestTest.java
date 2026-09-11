package com.prospecta.discovery.domain;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PeopleSearchRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should validate valid PeopleSearchRequest with default values")
    void shouldValidateValidRequest() {
        PeopleSearchRequest request = PeopleSearchRequest.builder()
                .jobTitles(List.of("CEO", "Directeur commercial"))
                .country("SN")
                .city("Dakar")
                .page(0)
                .size(25)
                .build();

        Set<ConstraintViolation<PeopleSearchRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
        assertThat(request.size()).isEqualTo(25);
        assertThat(request.page()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should reject size exceeding maximum limit of 50")
    void shouldRejectSizeOver50() {
        PeopleSearchRequest request = PeopleSearchRequest.builder()
                .country("SN")
                .page(0)
                .size(100)
                .build();

        Set<ConstraintViolation<PeopleSearchRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("maximale");
    }

    @Test
    @DisplayName("Should default negative page or invalid size in constructor")
    void shouldHandleDefaultValuesInConstructor() {
        PeopleSearchRequest request = new PeopleSearchRequest(
                "Mamadou", "Diop", null, "Gainde", null, "SN", "Dakar", "Tech", null, null, -1, 0
        );

        assertThat(request.page()).isEqualTo(0);
        assertThat(request.size()).isEqualTo(25);
        assertThat(request.jobTitles()).isEmpty();
    }
}
