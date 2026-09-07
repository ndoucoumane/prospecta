package com.prospecta.ai.repository;

import com.prospecta.ai.domain.AiPromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiPromptTemplateRepository extends JpaRepository<AiPromptTemplate, UUID> {

    Optional<AiPromptTemplate> findByNameAndActiveTrue(String name);

    Optional<AiPromptTemplate> findByNameAndVersion(String name, String version);
}
