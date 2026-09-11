package com.prospecta.enrichment.domain;

import com.prospecta.prospect.domain.Prospect;

import java.util.Optional;

/**
 * Provider abstraction for contact & B2B enrichment.
 * Decouples Prospecta from any single enrichment vendor (Apollo, etc.).
 */
public interface EnrichmentProvider {

    Optional<EnrichedProspectData> enrichPerson(Prospect prospect);

    String getProviderName();
}
