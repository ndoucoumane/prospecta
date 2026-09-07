# ADR-002: Keycloak comme Fournisseur d'Identité (IdP) OIDC

## Statut
Accepté

## Contexte
Le SaaS Prospecta nécessite une gestion sécurisée des identités, du SSO, du renouvellement de tokens et de la fédération d'identités sans compromettre la sécurité des mots de passe.

## Décision
Utiliser **Keycloak** comme Identity Provider (IdP) :
- Keycloak prend en charge l'enregistrement, l'authentification OIDC, la réinitialisation de mot de passe et l'émission des JWT signés.
- Spring Security agit en **Resource Server** pur : il valide la signature JWT via l'URI JWKS Keycloak et extrait les rôles de realm.
- Le backend Prospecta stocke uniquement un `UserProfile` associé au `keycloakSubject` (claim `sub`) et gère les permissions métier applicatives et le cloisonnement multi-tenant.
- Aucun mot de passe utilisateur n'est stocké dans la base PostgreSQL de l'application.

## Conséquences
- Sécurité renforcée dès le MVP.
- Support natif futur pour le SSO entreprise (Google Workspace, Microsoft Entra ID).
