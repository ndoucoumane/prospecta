package com.prospecta.discovery.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DiscoveryErrorCode {
    PROVIDER_UNAVAILABLE("PROVIDER_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "Le fournisseur de données B2B est temporairement indisponible"),
    RATE_LIMITED("RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS, "Limite de requêtes atteinte auprès du fournisseur de données"),
    INVALID_REQUEST("INVALID_REQUEST", HttpStatus.BAD_REQUEST, "Les critères de recherche fournis sont invalides"),
    AUTHENTICATION_ERROR("AUTHENTICATION_ERROR", HttpStatus.BAD_GATEWAY, "Erreur d'authentification avec le fournisseur de données externe"),
    NO_RESULTS("NO_RESULTS", HttpStatus.OK, "Aucun résultat trouvé pour les critères sélectionnés"),
    PROVIDER_ERROR("PROVIDER_ERROR", HttpStatus.BAD_GATEWAY, "Erreur inattendue retournée par le fournisseur de données"),
    TIMEOUT("TIMEOUT", HttpStatus.GATEWAY_TIMEOUT, "Délai d'attente dépassé lors de la requête vers le fournisseur de données");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
