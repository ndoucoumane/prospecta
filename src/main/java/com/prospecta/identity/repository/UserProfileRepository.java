package com.prospecta.identity.repository;

import com.prospecta.identity.domain.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByKeycloakSubject(String keycloakSubject);

    Optional<UserProfile> findByEmail(String email);

    @Query("SELECT u FROM UserProfile u JOIN FETCH u.organization WHERE u.keycloakSubject = :keycloakSubject")
    Optional<UserProfile> findByKeycloakSubjectWithOrganization(@Param("keycloakSubject") String keycloakSubject);

    @Query("SELECT u FROM UserProfile u WHERE u.organization.id = :organizationId")
    Page<UserProfile> findAllByOrganizationId(@Param("organizationId") UUID organizationId, Pageable pageable);

    boolean existsByEmail(String email);
}
